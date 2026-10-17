package com.bookworm.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.Address;
import com.bookworm.address.AddressRequest;
import com.bookworm.book.Book;
import com.bookworm.book.BookQuantity;
import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.pricing.PriceBreakdown;
import com.bookworm.publisher.Publisher;
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderTest {

    private static final Instant PLACED_AT = Instant.parse("2025-01-01T10:00:00Z");

    private User user;
    private Address address;
    private Book book;

    @BeforeEach
    void setUp() {
        user = new User("Arjun", "a@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.addGiftPoints(100);
        address = new Address(user, new AddressRequest("A", "B", "12 MG Road", "Mumbai", null, "400001",
                "India", "+919876543210", null));
        book = Book.builder("Title", "Author", new BigDecimal("100.00"), new Category("Fiction"),
                new Publisher("Penguin")).stockQuantity(10).build();
        ReflectionTestUtils.setField(book, "id", 5L);
    }

    @Test
    void placeReservesStockSpendsPointsAndSnapshotsPrice() {
        Order order = place(3, 40);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(book.getStockQuantity()).isEqualTo(7);
        assertThat(user.getGiftPoints()).isEqualTo(60);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100.00");
        assertThat(order.getItems().get(0).lineTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void placeComputesTotalFromPricesAndDiscount() {
        Order order = place(3, 40);

        assertThat(order.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(order.getTax()).isEqualByComparingTo("36.00");
        assertThat(order.getDeliveryCharges()).isEqualByComparingTo("40.00");
        assertThat(order.getDiscount()).isEqualByComparingTo("40.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("336.00");
    }

    @Test
    void placeFailsWithoutEnoughStock() {
        assertThatThrownBy(() -> place(11, 0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void placeFailsWithoutEnoughGiftPoints() {
        assertThatThrownBy(() -> place(1, 101))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("gift points");
    }

    @Test
    void cancelRestoresStockAndGiftPoints() {
        Order order = place(3, 40);

        order.cancel(PLACED_AT.plusSeconds(3600));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(book.getStockQuantity()).isEqualTo(10);
        assertThat(user.getGiftPoints()).isEqualTo(100);
    }

    @Test
    void cancelIsAllowedExactlyAtDeadline() {
        Order order = place(1, 0);

        order.cancel(order.cancellationDeadline());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelAfterDeadlineIsRejectedAndChangesNothing() {
        Order order = place(3, 40);

        assertThatThrownBy(() -> order.cancel(order.cancellationDeadline().plusSeconds(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("48-hour");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(book.getStockQuantity()).isEqualTo(7);
        assertThat(user.getGiftPoints()).isEqualTo(60);
    }

    @Test
    void cancellingTwiceIsRejectedSoStockIsNotRestoredTwice() {
        Order order = place(3, 0);
        order.cancel(PLACED_AT);

        assertThatThrownBy(() -> order.cancel(PLACED_AT)).isInstanceOf(BusinessRuleException.class);
        assertThat(book.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        Order order = place(1, 0);
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED);

        assertThatThrownBy(() -> order.cancel(PLACED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SHIPPED");
    }

    @Test
    void cancellationDeadlineIsFortyEightHoursAfterPlacement() {
        assertThat(place(1, 0).cancellationDeadline()).isEqualTo(Instant.parse("2025-01-03T10:00:00Z"));
    }

    @Test
    void linesExposeBooksAndQuantitiesForBuyAgain() {
        List<BookQuantity> lines = place(2, 0).lines();

        assertThat(lines).containsExactly(new BookQuantity(book, 2));
    }

    @Test
    void confirmPaymentConfirmsOrderCreditsPointsAndRecordsSales() {
        Order order = placeEarning(3, 0, 30);

        order.confirmPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(user.getGiftPoints()).isEqualTo(130);
        assertThat(book.getCopiesSold()).isEqualTo(3);
    }

    @Test
    void confirmPaymentTwiceIsRejectedSoPointsAreNotCreditedTwice() {
        Order order = placeEarning(3, 0, 30);
        order.confirmPayment();

        assertThatThrownBy(order::confirmPayment).isInstanceOf(BusinessRuleException.class);
        assertThat(user.getGiftPoints()).isEqualTo(130);
        assertThat(book.getCopiesSold()).isEqualTo(3);
    }

    @Test
    void requireAwaitingPaymentRejectsOrdersInOtherStatuses() {
        Order order = place(1, 0);
        order.requireAwaitingPayment();
        order.cancel(PLACED_AT);

        assertThatThrownBy(order::requireAwaitingPayment)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void cancellingPaidOrderReversesEarnedPointsAndSales() {
        Order order = placeEarning(3, 40, 30);
        order.confirmPayment();

        order.cancel(PLACED_AT.plusSeconds(3600));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(book.getStockQuantity()).isEqualTo(10);
        assertThat(book.getCopiesSold()).isZero();
        assertThat(user.getGiftPoints()).isEqualTo(100);
    }

    @Test
    void cancellingPaidOrderNeverDrivesPointsBelowZero() {
        Order order = placeEarning(3, 0, 30);
        order.confirmPayment();
        user.deductGiftPoints(user.getGiftPoints() - 10);

        order.cancel(PLACED_AT);

        assertThat(user.getGiftPoints()).isZero();
    }

    private Order place(int quantity, int points) {
        return placeEarning(quantity, points, 0);
    }

    private Order placeEarning(int quantity, int points, int earned) {
        PriceBreakdown prices = new PriceBreakdown(
                new BigDecimal("100.00").multiply(BigDecimal.valueOf(quantity)).setScale(2),
                new BigDecimal("12.00").multiply(BigDecimal.valueOf(quantity)).setScale(2),
                new BigDecimal("40.00"));
        return Order.place(user, address, List.of(new BookQuantity(book, quantity)), prices, points,
                BigDecimal.valueOf(points).setScale(2), earned, PLACED_AT);
    }
}
