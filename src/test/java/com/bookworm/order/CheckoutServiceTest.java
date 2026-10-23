package com.bookworm.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.bookworm.address.Address;
import com.bookworm.address.AddressRequest;
import com.bookworm.address.AddressService;
import com.bookworm.book.Book;
import com.bookworm.cart.AddCartItemRequest;
import com.bookworm.cart.CartResponse;
import com.bookworm.cart.CartService;
import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;
import com.bookworm.coupon.CouponService;
import com.bookworm.coupon.Coupon;
import com.bookworm.coupon.DiscountType;
import com.bookworm.giftpoints.GiftPointsPolicy;
import com.bookworm.giftpoints.GiftPointsProperties;
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
 * Exercises placing, listing, cancelling and re-buying orders, plus address deletion rules, against a real
 * persistence context so that mappings, cascades and transactional rollback behaviour are covered.
 */
@DataJpaTest
@Import({CheckoutService.class, OrderService.class, CartService.class, AddressService.class,
        OrderAddressUsageChecker.class, PricingService.class, GiftPointsPolicy.class, CouponService.class,
        CheckoutServiceTest.TestBeans.class})
class CheckoutServiceTest {

    private static final Instant NOW = Instant.parse("2025-03-01T12:00:00Z");

    @TestConfiguration
    static class TestBeans {
        @Bean
        PricingProperties pricingProperties() {
            return new PricingProperties(new BigDecimal("0.12"), new BigDecimal("500.00"), new BigDecimal("40.00"));
        }

        @Bean
        GiftPointsProperties giftPointsProperties() {
            return new GiftPointsProperties(10);
        }

        @Bean
        MutableClock clock() {
            return new MutableClock(NOW);
        }
    }

    /** A clock tests can move forward to cross the cancellation deadline. */
    static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(java.time.Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private MutableClock clock;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Address address;
    private Book cheap;
    private Book pricey;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        user.addGiftPoints(100);
        address = em.persist(new Address(user, new AddressRequest("Arjun", "Patel", "12 MG Road", "Mumbai", null,
                "400001", "India", "+919876543210", null)));
        Category category = em.persist(new Category("Fiction"));
        Publisher publisher = em.persist(new Publisher("Penguin"));
        cheap = em.persist(Book.builder("Cheap", "A", new BigDecimal("149.00"), category, publisher)
                .stockQuantity(10).build());
        pricey = em.persist(Book.builder("Pricey", "B", new BigDecimal("210.00"), category, publisher)
                .stockQuantity(2).build());
        em.flush();
    }

    @Test
    void placeOrderConvertsCartIntoOrderAndEmptiesCart() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 2));
        cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 1));

        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 50, null));

        assertThat(order.id()).isNotNull();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.items()).hasSize(2);
        assertThat(order.items().get(0).unitPrice()).isEqualByComparingTo("149.00");
        assertThat(order.subtotal()).isEqualByComparingTo("508.00");
        assertThat(order.tax()).isEqualByComparingTo("60.96");
        assertThat(order.deliveryCharges()).isEqualByComparingTo("0.00");
        assertThat(order.discount()).isEqualByComparingTo("50.00");
        assertThat(order.totalAmount()).isEqualByComparingTo("518.96");
        assertThat(order.giftPointsRedeemed()).isEqualTo(50);
        assertThat(order.giftPointsEarned()).isEqualTo(51);
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.cancellationDeadline()).isEqualTo(NOW.plus(java.time.Duration.ofHours(48)));
        assertThat(order.address().city()).isEqualTo("Mumbai");

        assertThat(cartService.getCart(user.getId()).items()).isEmpty();
        em.flush();
        em.clear();
        assertThat(em.find(Book.class, cheap.getId()).getStockQuantity()).isEqualTo(8);
        assertThat(em.find(Book.class, pricey.getId()).getStockQuantity()).isEqualTo(1);
        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isEqualTo(50);
    }

    @Test
    void orderKeepsPriceSnapshotWhenBookPriceLaterChanges() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));
        em.flush();
        em.clear();

        ReflectionPrice.set(em.find(Book.class, cheap.getId()), "999.00");
        em.flush();
        em.clear();

        assertThat(orderService.get(user.getId(), order.id()).items().get(0).unitPrice()).isEqualByComparingTo("149.00");
    }

    @Test
    void emptyCartIsRejected() {
        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void unknownAddressIsNotFound() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(9999L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anotherUsersAddressIsReportedAsNotFound() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));
        Address foreign = em.persist(new Address(other, new AddressRequest("O", "P", "1 Other Street", "Delhi", null,
                "110001", "India", "+919876543211", null)));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(foreign.getId(), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void redeemingMoreThanBalanceIsRejected() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 5));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 101, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("gift points");
    }

    @Test
    void redeemingMoreThanSubtotalIsRejected() {
        user.addGiftPoints(1000);
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 150, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("subtotal");
    }

    @Test
    void percentCouponIsAppliedToSubtotalBeforeGiftPointsAndRecordedOnTheOrder() {
        persistCoupon(Coupon.builder("SAVE20", DiscountType.PERCENT, new BigDecimal("20")));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 50, "SAVE20"));

        assertThat(order.couponCode()).isEqualTo("SAVE20");
        assertThat(order.couponDiscount()).isEqualByComparingTo("29.80");
        assertThat(order.discount()).isEqualByComparingTo("79.80");
        // 149.00 subtotal + 17.88 tax + 40.00 delivery - 79.80 discount
        assertThat(order.totalAmount()).isEqualByComparingTo("127.08");
        assertThat(order.giftPointsEarned()).isEqualTo(12);
    }

    @Test
    void couponCodeIsCaseAndWhitespaceInsensitiveAndCountsItsUse() {
        Coupon coupon = persistCoupon(Coupon.builder("SAVE100", DiscountType.FIXED, new BigDecimal("100")));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "  save100 "));

        assertThat(order.couponCode()).isEqualTo("SAVE100");
        assertThat(order.discount()).isEqualByComparingTo("100.00");
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
    }

    @Test
    void couponDiscountNeverExceedsSubtotal() {
        persistCoupon(Coupon.builder("BIG", DiscountType.FIXED, new BigDecimal("1000")));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "BIG"));

        assertThat(order.couponDiscount()).isEqualByComparingTo("149.00");
        // Only tax and delivery remain payable.
        assertThat(order.totalAmount()).isEqualByComparingTo("57.88");
    }

    @Test
    void giftPointsMayOnlyCoverWhatTheCouponLeavesOfTheSubtotal() {
        persistCoupon(Coupon.builder("SAVE100", DiscountType.FIXED, new BigDecimal("100")));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 50, "SAVE100")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("subtotal");
    }

    @Test
    void unknownCouponIsRejected() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "NOPE")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void couponBelowMinimumOrderAmountIsRejectedAndNotUsedUp() {
        Coupon coupon = persistCoupon(Coupon.builder("BIGSPENDER", DiscountType.FIXED, new BigDecimal("50"))
                .minOrderAmount(new BigDecimal("500")));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "BIGSPENDER")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum order");
        assertThat(coupon.getTimesUsed()).isZero();
    }

    @Test
    void expiredAndExhaustedCouponsAreRejected() {
        persistCoupon(Coupon.builder("OLD", DiscountType.FIXED, new BigDecimal("10")).validUntil(NOW.minusSeconds(1)));
        persistCoupon(Coupon.builder("ONCE", DiscountType.FIXED, new BigDecimal("10")).usageLimit(1));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "OLD")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        // The test shares one transaction, so the cart drained by the rejected attempt is not rolled back.
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "ONCE"));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "ONCE")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void cancellingAnOrderGivesTheCouponUseBack() {
        Coupon coupon = persistCoupon(Coupon.builder("ONCE", DiscountType.FIXED, new BigDecimal("10")).usageLimit(1));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "ONCE"));
        assertThat(coupon.getTimesUsed()).isEqualTo(1);

        orderService.cancel(user.getId(), placed.id());

        assertThat(coupon.getTimesUsed()).isZero();
    }

    @Test
    void blankCouponIsTreatedAsAbsent() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        OrderResponse order = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, "  "));

        assertThat(order.discount()).isEqualByComparingTo("0.00");
        assertThat(order.couponCode()).isNull();
    }

    private Coupon persistCoupon(Coupon.Builder builder) {
        Coupon coupon = em.persist(builder.build());
        em.flush();
        return coupon;
    }

    @Test
    void stockTakenByAnotherOrderSincePlacingInCartIsRejected() {
        cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 2));
        pricey.reserveStock(1);
        em.flush();

        assertThatThrownBy(() -> checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 1 copies");
    }

    @Test
    void cancelRestoresStockAndGiftPoints() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 2));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), 30, null));
        clock.advance(java.time.Duration.ofHours(47));

        OrderResponse cancelled = orderService.cancel(user.getId(), placed.id());
        em.flush();
        em.clear();

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(em.find(Book.class, cheap.getId()).getStockQuantity()).isEqualTo(10);
        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isEqualTo(100);
    }

    @Test
    void cancelAfterFortyEightHoursIsRejected() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));
        clock.advance(java.time.Duration.ofHours(48).plusSeconds(1));

        assertThatThrownBy(() -> orderService.cancel(user.getId(), placed.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("48-hour");
    }

    @Test
    void anotherUserCannotViewOrCancelOrder() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));

        assertThatThrownBy(() -> orderService.get(other.getId(), placed.id())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> orderService.cancel(other.getId(), placed.id())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> orderService.buyAgain(other.getId(), placed.id())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unknownOrderIsNotFound() {
        assertThatThrownBy(() -> orderService.get(user.getId(), 4242L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listIsPagedNewestFirstAndFilterableByStatus() {
        OrderResponse first = placeSingleBookOrder();
        clock.advance(java.time.Duration.ofMinutes(5));
        OrderResponse second = placeSingleBookOrder();
        orderService.cancel(user.getId(), first.id());

        PageResponse<OrderResponse> all = orderService.list(user.getId(), null, 0, 10);
        PageResponse<OrderResponse> cancelledOnly = orderService.list(user.getId(), OrderStatus.CANCELLED, 0, 10);
        PageResponse<OrderResponse> firstPage = orderService.list(user.getId(), null, 0, 1);

        assertThat(all.content()).extracting(OrderResponse::id).containsExactly(second.id(), first.id());
        assertThat(cancelledOnly.content()).extracting(OrderResponse::id).containsExactly(first.id());
        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
    }

    @Test
    void listOnlyReturnsOwnOrders() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));
        placeSingleBookOrder();

        assertThat(orderService.list(other.getId(), null, 0, 10).content()).isEmpty();
    }

    @Test
    void buyAgainAddsBooksBackAndSkipsOutOfStockOnes() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 2));
        cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 2));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));
        assertThat(em.find(Book.class, pricey.getId()).getStockQuantity()).isZero();

        CartResponse cart = orderService.buyAgain(user.getId(), placed.id());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).book().title()).isEqualTo("Cheap");
        assertThat(cart.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void buyAgainFailsWhenNothingIsAvailable() {
        cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 2));
        OrderResponse placed = checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));

        assertThatThrownBy(() -> orderService.buyAgain(user.getId(), placed.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addressUsedByAnOrderCannotBeDeleted() {
        placeSingleBookOrder();

        assertThatThrownBy(() -> addressService.delete(user.getId(), address.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unusedAddressCanBeDeleted() {
        addressService.delete(user.getId(), address.getId());
        em.flush();

        assertThat(addressService.list(user.getId())).isEmpty();
    }

    @Test
    void addressOfAnotherUserCannotBeAccessed() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));

        assertThatThrownBy(() -> addressService.get(other.getId(), address.getId())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> addressService.delete(other.getId(), address.getId())).isInstanceOf(ForbiddenException.class);
    }

    private OrderResponse placeSingleBookOrder() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        return checkoutService.placeOrder(user.getId(), new CreateOrderRequest(address.getId(), null, null));
    }

    private static final class ReflectionPrice {
        static void set(Book book, String price) {
            org.springframework.test.util.ReflectionTestUtils.setField(book, "price", new BigDecimal(price));
        }
    }
}
