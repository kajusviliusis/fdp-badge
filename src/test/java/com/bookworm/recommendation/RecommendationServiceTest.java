package com.bookworm.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.Address;
import com.bookworm.address.AddressRequest;
import com.bookworm.book.Book;
import com.bookworm.book.BookQuantity;
import com.bookworm.book.BookSummaryResponse;
import com.bookworm.category.Category;
import com.bookworm.order.Order;
import com.bookworm.order.OrderStatus;
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
@Import(RecommendationService.class)
class RecommendationServiceTest {

    @Autowired
    private RecommendationService service;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Address address;
    private Category fiction;
    private Category science;
    private Category cooking;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        address = em.persist(new Address(user, new AddressRequest("A", "B", "12 MG Road", "Mumbai", null, "400001",
                "India", "+919876543210", null)));
        fiction = em.persist(new Category("Fiction"));
        science = em.persist(new Category("Science"));
        cooking = em.persist(new Category("Cooking"));
        publisher = em.persist(new Publisher("Penguin"));
    }

    private Book book(String title, String author, Category category, int stock, int sold) {
        return em.persist(Book.builder(title, author, new BigDecimal("100.00"), category, publisher)
                .stockQuantity(stock).copiesSold(sold).build());
    }

    private void ordered(OrderStatus status, Book... books) {
        PriceBreakdown prices = new PriceBreakdown(new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("40.00"));
        List<BookQuantity> lines = java.util.Arrays.stream(books).map(b -> new BookQuantity(b, 1)).toList();
        Order order = Order.place(user, address, lines, prices, 0, BigDecimal.ZERO.setScale(2), 0,
                Instant.parse("2025-03-01T12:00:00Z"));
        ReflectionTestUtils.setField(order, "status", status);
        em.persist(order);
        em.flush();
    }

    private List<String> titles(int limit) {
        em.flush();
        em.clear();
        return service.recommendFor(user.getId(), limit).stream().map(BookSummaryResponse::title).toList();
    }

    @Test
    void withoutHistoryTheMostPopularInStockBooksAreSuggested() {
        book("Modest", "A", fiction, 5, 10);
        book("Hit", "B", science, 5, 500);
        book("Sold Out Hit", "C", cooking, 0, 900);

        assertThat(titles(10)).containsExactly("Hit", "Modest");
    }

    @Test
    void booksByAnAuthorTheUserHasBoughtComeFirst() {
        Book bought = book("Bought", "Fav Author", fiction, 1, 1);
        book("Popular Fiction", "Other", fiction, 5, 1000);
        book("Same Author Elsewhere", "Fav Author", science, 5, 1);
        ordered(OrderStatus.CONFIRMED, bought);

        assertThat(titles(10)).containsExactly("Same Author Elsewhere", "Popular Fiction");
    }

    @Test
    void booksInABoughtCategoryAreRankedByPopularity() {
        Book bought = book("Bought", "X", fiction, 1, 1);
        book("Less Popular", "Y", fiction, 5, 5);
        book("More Popular", "Z", fiction, 5, 50);
        ordered(OrderStatus.DELIVERED, bought);

        assertThat(titles(2)).containsExactly("More Popular", "Less Popular");
    }

    @Test
    void alreadyOrderedBooksAreNeverSuggested() {
        Book bought = book("Bought", "X", fiction, 5, 999);
        book("Fresh", "Y", fiction, 5, 1);
        ordered(OrderStatus.PENDING_PAYMENT, bought);

        assertThat(titles(10)).containsExactly("Fresh");
    }

    @Test
    void cancelledOrdersDoNotInfluenceOrExcludeAnything() {
        Book cancelled = book("Cancelled", "Fav Author", fiction, 5, 1);
        book("Cooking Hit", "Chef", cooking, 5, 100);
        book("Fiction Meh", "Other", fiction, 5, 2);
        ordered(OrderStatus.CANCELLED, cancelled);

        assertThat(titles(10)).containsExactly("Cooking Hit", "Fiction Meh", "Cancelled");
    }

    @Test
    void outOfStockBooksAreSkippedEvenWhenTheyMatchTheUsersTaste() {
        Book bought = book("Bought", "Fav", fiction, 5, 1);
        book("Sold Out", "Fav", fiction, 0, 999);
        book("Available", "Other", fiction, 5, 1);
        ordered(OrderStatus.CONFIRMED, bought);

        assertThat(titles(10)).containsExactly("Available");
    }

    @Test
    void whenTasteMatchesRunOutPopularBooksFillTheRemainingSlots() {
        Book bought = book("Bought", "Fav", fiction, 5, 1);
        book("Matches Taste", "Fav", science, 5, 1);
        book("Popular Elsewhere", "Chef", cooking, 5, 300);
        ordered(OrderStatus.CONFIRMED, bought);

        assertThat(titles(3)).containsExactly("Matches Taste", "Popular Elsewhere");
    }

    @Test
    void limitIsRespectedAndNoBookAppearsTwice() {
        Book bought = book("Bought", "Fav", fiction, 5, 1);
        for (int i = 0; i < 5; i++) {
            book("Fiction " + i, "Writer " + i, fiction, 5, i);
        }
        ordered(OrderStatus.CONFIRMED, bought);

        List<String> titles = titles(3);

        assertThat(titles).hasSize(3).doesNotHaveDuplicates();
    }

    @Test
    void anotherUsersHistoryIsIgnored() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));
        Book fictionBook = book("Fiction Book", "Fav", fiction, 5, 1);
        book("Cooking Book", "Chef", cooking, 5, 1000);
        PriceBreakdown prices = new PriceBreakdown(new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("40.00"));
        em.persist(Order.place(other, address, List.of(new BookQuantity(fictionBook, 1)), prices, 0,
                BigDecimal.ZERO.setScale(2), 0, Instant.parse("2025-03-01T12:00:00Z")));

        assertThat(titles(10)).containsExactly("Cooking Book", "Fiction Book");
    }
}
