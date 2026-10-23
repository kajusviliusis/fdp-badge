package com.bookworm.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.bookworm.address.Address;
import com.bookworm.address.AddressRequest;
import com.bookworm.address.AddressService;
import com.bookworm.address.AddressUsageChecker;
import com.bookworm.book.Book;
import com.bookworm.cart.AddCartItemRequest;
import com.bookworm.cart.CartService;
import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.coupon.CouponService;
import com.bookworm.giftpoints.GiftPointsPolicy;
import com.bookworm.giftpoints.GiftPointsProperties;
import com.bookworm.order.CheckoutService;
import com.bookworm.order.CreateOrderRequest;
import com.bookworm.order.OrderResponse;
import com.bookworm.order.OrderService;
import com.bookworm.order.OrderStatus;
import com.bookworm.pricing.PricingProperties;
import com.bookworm.pricing.PricingService;
import com.bookworm.publisher.Publisher;
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Exercises paying for orders against a real persistence context, so order confirmation, gift point crediting
 * and sales tracking are verified end to end.
 */
@DataJpaTest
@Import({PaymentService.class, SimulatedPaymentGateway.class, CheckoutService.class, OrderService.class,
        CartService.class, AddressService.class, PricingService.class, GiftPointsPolicy.class, CouponService.class,
        PaymentServiceTest.TestBeans.class})
class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2025-03-01T12:00:00Z");

    @TestConfiguration
    static class TestBeans {
        @Bean
        AddressUsageChecker addressUsageChecker() {
            return addressId -> false;
        }

        @Bean
        PricingProperties pricingProperties() {
            return new PricingProperties(new BigDecimal("0.12"), new BigDecimal("500.00"), new BigDecimal("40.00"));
        }

        @Bean
        GiftPointsProperties giftPointsProperties() {
            return new GiftPointsProperties(10);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Book book;
    private OrderResponse order;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        Address address = em.persist(new Address(user, new AddressRequest("Arjun", "Patel", "12 MG Road", "Mumbai",
                null, "400001", "India", "+919876543210", null)));
        Category category = em.persist(new Category("Fiction"));
        Publisher publisher = em.persist(new Publisher("Penguin"));
        book = em.persist(Book.builder("Cheap", "A", new BigDecimal("149.00"), category, publisher)
                .stockQuantity(10).build());
        em.flush();

        cartService.addItem(user.getId(), new AddCartItemRequest(book.getId(), 2));
        order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));
    }

    private static PaymentRequest validCard() {
        return new PaymentRequest(PaymentMethod.CREDIT_CARD,
                new CardDetails("4111111111111111", "Arjun Patel", "123", "12/2030"), null);
    }

    private static PaymentRequest declinedUpi() {
        return new PaymentRequest(PaymentMethod.UPI, null, new UpiDetails("arjun@fail"));
    }

    @Test
    void successfulPaymentConfirmsOrderCreditsPointsAndRecordsSales() {
        PaymentResponse payment = paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.orderId()).isEqualTo(order.id());
        assertThat(payment.method()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.amount()).isEqualByComparingTo(order.totalAmount());
        assertThat(payment.transactionReference()).startsWith("TXN-20250301-");
        assertThat(orderService.get(user.getId(), order.id()).status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isEqualTo(order.giftPointsEarned());
        assertThat(em.find(Book.class, book.getId()).getCopiesSold()).isEqualTo(2);
    }

    @Test
    void declinedPaymentIsReportedAsFailedAndLeavesOrderAwaitingPayment() {
        PaymentResponse payment = paymentService.pay(user.getId(), order.id(), declinedUpi());
        em.flush();
        em.clear();

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderService.get(user.getId(), order.id()).status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isZero();
        assertThat(em.find(Book.class, book.getId()).getCopiesSold()).isZero();
    }

    @Test
    void retryAfterDeclineReusesThePaymentAndSucceeds() {
        PaymentResponse failed = paymentService.pay(user.getId(), order.id(), declinedUpi());
        em.flush();
        em.clear();

        PaymentResponse retried = paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        assertThat(retried.id()).isEqualTo(failed.id());
        assertThat(retried.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(retried.method()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(retried.transactionReference()).isNotEqualTo(failed.transactionReference());
        assertThat(orderService.get(user.getId(), order.id()).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void alreadyPaidOrderCannotBePaidAgain() {
        paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> paymentService.pay(user.getId(), order.id(), validCard()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CONFIRMED");
        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isEqualTo(order.giftPointsEarned());
    }

    @Test
    void cancelledOrderCannotBePaid() {
        orderService.cancel(user.getId(), order.id());

        assertThatThrownBy(() -> paymentService.pay(user.getId(), order.id(), validCard()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void anotherUserCannotPayForOrNeverSeeThePayment() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));

        assertThatThrownBy(() -> paymentService.pay(other.getId(), order.id(), validCard()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> paymentService.get(other.getId(), order.id()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unknownOrderIsNotFound() {
        assertThatThrownBy(() -> paymentService.pay(user.getId(), 4242L, validCard()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBeforePayingIsNotFound() {
        assertThatThrownBy(() -> paymentService.get(user.getId(), order.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReturnsTheRecordedPayment() {
        PaymentResponse paid = paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        assertThat(paymentService.get(user.getId(), order.id())).isEqualTo(paid);
    }

    @Test
    void orderResponseIncludesPaymentOnceMade() {
        assertThat(orderService.get(user.getId(), order.id()).payment()).isNull();

        paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        assertThat(orderService.get(user.getId(), order.id()).payment())
                .extracting(PaymentResponse::status)
                .isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void cancellingPaidOrderReversesEarnedPointsAndSales() {
        paymentService.pay(user.getId(), order.id(), validCard());
        em.flush();
        em.clear();

        orderService.cancel(user.getId(), order.id());
        em.flush();
        em.clear();

        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isZero();
        assertThat(em.find(Book.class, book.getId()).getCopiesSold()).isZero();
        assertThat(em.find(Book.class, book.getId()).getStockQuantity()).isEqualTo(10);
    }
}
