package com.bookworm.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.Address;
import com.bookworm.address.AddressRequest;
import com.bookworm.book.Book;
import com.bookworm.book.BookQuantity;
import com.bookworm.category.Category;
import com.bookworm.pricing.PriceBreakdown;
import com.bookworm.publisher.Publisher;
import com.bookworm.support.PostgresDataJpaTest;
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@PostgresDataJpaTest
@Import(OrderSalesReport.class)
class OrderSalesReportTest {

    private static final Instant FROM = Instant.parse("2025-03-01T00:00:00Z");
    private static final Instant UNTIL = Instant.parse("2025-04-01T00:00:00Z");

    @Autowired
    private OrderSalesReport report;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Address address;
    private Category category;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        address = em.persist(new Address(user, new AddressRequest("A", "B", "12 MG Road", "Mumbai", null, "400001",
                "India", "+919876543210", null)));
        category = em.persist(new Category("Fiction"));
        publisher = em.persist(new Publisher("Penguin"));
    }

    @Test
    void ranksBooksByCopiesPaidWithinThePeriodWithIdAsTieBreaker() {
        Book few = book("Few");
        Book many = book("Many");
        Book tiedFirst = book("Tied first");
        Book tiedSecond = book("Tied second");
        order(OrderStatus.CONFIRMED, FROM.plusSeconds(60), new BookQuantity(few, 1), new BookQuantity(many, 5));
        order(OrderStatus.DELIVERED, FROM.plusSeconds(120), new BookQuantity(tiedSecond, 2),
                new BookQuantity(tiedFirst, 2));

        // Equal totals are ordered by id, i.e. creation order.
        assertThat(report.topSellingBookIds(FROM, UNTIL, 10))
                .containsExactly(many.getId(), tiedFirst.getId(), tiedSecond.getId(), few.getId());
    }

    @Test
    void sumsCopiesAcrossOrdersAndAppliesTheLimit() {
        Book steady = book("Steady");
        Book bulk = book("Bulk");
        order(OrderStatus.CONFIRMED, FROM.plusSeconds(1), new BookQuantity(steady, 2));
        order(OrderStatus.SHIPPED, FROM.plusSeconds(2), new BookQuantity(steady, 2));
        order(OrderStatus.CONFIRMED, FROM.plusSeconds(3), new BookQuantity(bulk, 3));

        assertThat(report.topSellingBookIds(FROM, UNTIL, 1)).containsExactly(steady.getId());
    }

    @Test
    void ignoresPendingAndCancelledOrders() {
        Book book = book("Never sold");
        order(OrderStatus.PENDING_PAYMENT, null, new BookQuantity(book, 5));
        order(OrderStatus.CANCELLED, FROM.plusSeconds(60), new BookQuantity(book, 5));

        assertThat(report.topSellingBookIds(FROM, UNTIL, 10)).isEmpty();
    }

    @Test
    void periodStartIsInclusiveAndEndIsExclusive() {
        Book atStart = book("At start");
        Book atEnd = book("At end");
        Book before = book("Before");
        order(OrderStatus.CONFIRMED, FROM, new BookQuantity(atStart, 1));
        order(OrderStatus.CONFIRMED, UNTIL, new BookQuantity(atEnd, 9));
        order(OrderStatus.CONFIRMED, FROM.minusSeconds(1), new BookQuantity(before, 9));

        assertThat(report.topSellingBookIds(FROM, UNTIL, 10)).containsExactly(atStart.getId());
    }

    private Book book(String title) {
        return em.persist(Book.builder(title, "Author", new BigDecimal("100.00"), category, publisher)
                .stockQuantity(100).build());
    }

    private void order(OrderStatus status, Instant paidAt, BookQuantity... lines) {
        PriceBreakdown prices = new PriceBreakdown(new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("40.00"));
        Order order = Order.place(user, address, List.of(lines), prices, 0, BigDecimal.ZERO.setScale(2), 0,
                Instant.parse("2025-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "paidAt", paidAt);
        em.persist(order);
        em.flush();
    }
}
