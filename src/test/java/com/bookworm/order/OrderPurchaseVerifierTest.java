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
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(OrderPurchaseVerifier.class)
class OrderPurchaseVerifierTest {

    @Autowired
    private OrderPurchaseVerifier verifier;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Address address;
    private Book book;
    private Book otherBook;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        address = em.persist(new Address(user, new AddressRequest("A", "B", "12 MG Road", "Mumbai", null, "400001",
                "India", "+919876543210", null)));
        Category category = em.persist(new Category("Fiction"));
        Publisher publisher = em.persist(new Publisher("Penguin"));
        book = em.persist(Book.builder("Bought", "A", new BigDecimal("100.00"), category, publisher)
                .stockQuantity(10).build());
        otherBook = em.persist(Book.builder("Other", "B", new BigDecimal("100.00"), category, publisher)
                .stockQuantity(10).build());
    }

    private void orderWithStatus(User buyer, OrderStatus status) {
        PriceBreakdown prices = new PriceBreakdown(new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("40.00"));
        Order order = Order.place(buyer, address, List.of(new BookQuantity(book, 1)), prices, 0,
                BigDecimal.ZERO.setScale(2), 0, Instant.parse("2025-03-01T12:00:00Z"));
        ReflectionTestUtils.setField(order, "status", status);
        em.persist(order);
        em.flush();
    }

    @Test
    void paidOrdersCountAsPurchases() {
        for (OrderStatus status : List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            User buyer = em.persist(new User("Buyer " + status, status + "@example.com", "hash"));
            orderWithStatus(buyer, status);

            assertThat(verifier.hasPurchased(buyer.getId(), book.getId())).as(status.name()).isTrue();
        }
    }

    @Test
    void unpaidAndCancelledOrdersDoNotCountAsPurchases() {
        User pending = em.persist(new User("Pending", "pending@example.com", "hash"));
        User cancelled = em.persist(new User("Cancelled", "cancelled@example.com", "hash"));
        orderWithStatus(pending, OrderStatus.PENDING_PAYMENT);
        orderWithStatus(cancelled, OrderStatus.CANCELLED);

        assertThat(verifier.hasPurchased(pending.getId(), book.getId())).isFalse();
        assertThat(verifier.hasPurchased(cancelled.getId(), book.getId())).isFalse();
    }

    @Test
    void purchaseIsSpecificToTheBookAndTheBuyer() {
        User stranger = em.persist(new User("Stranger", "stranger@example.com", "hash"));
        orderWithStatus(user, OrderStatus.CONFIRMED);

        assertThat(verifier.hasPurchased(user.getId(), otherBook.getId())).isFalse();
        assertThat(verifier.hasPurchased(stranger.getId(), book.getId())).isFalse();
    }
}
