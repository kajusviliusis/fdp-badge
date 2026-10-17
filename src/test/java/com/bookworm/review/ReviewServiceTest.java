package com.bookworm.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.bookworm.book.Book;
import com.bookworm.category.Category;
import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;
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

@DataJpaTest
@Import({ReviewService.class, ReviewServiceTest.TestBeans.class})
class ReviewServiceTest {

    /** Purchases are granted per test through {@link #purchases}. */
    static class StubPurchaseVerifier implements PurchaseVerifier {
        final Set<String> purchases = new HashSet<>();

        @Override
        public boolean hasPurchased(Long userId, Long bookId) {
            return purchases.contains(userId + ":" + bookId);
        }
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        StubPurchaseVerifier purchaseVerifier() {
            return new StubPurchaseVerifier();
        }
    }

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private StubPurchaseVerifier purchases;
    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private Book book;

    @BeforeEach
    void setUp() {
        purchases.purchases.clear();
        alice = em.persist(new User("Alice Smith", "alice@example.com", "hash"));
        bob = em.persist(new User("Bob Jones", "bob@example.com", "hash"));
        Category category = em.persist(new Category("Fiction"));
        Publisher publisher = em.persist(new Publisher("Penguin"));
        book = em.persist(Book.builder("Title", "Author", new BigDecimal("100.00"), category, publisher).build());
        em.flush();
    }

    private void bought(User user) {
        purchases.purchases.add(user.getId() + ":" + book.getId());
    }

    private static ReviewRequest review(int rating) {
        return new ReviewRequest(rating, "A genuinely good read, recommended.");
    }

    @Test
    void buyerCanReviewAndResponseDescribesTheReview() {
        bought(alice);

        ReviewResponse response = reviewService.create(alice.getId(), book.getId(), review(5));

        assertThat(response.id()).isNotNull();
        assertThat(response.bookId()).isEqualTo(book.getId());
        assertThat(response.userId()).isEqualTo(alice.getId());
        assertThat(response.reviewerName()).isEqualTo("Alice Smith");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("A genuinely good read, recommended.");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void reviewingUpdatesTheBooksAverageAndCount() {
        bought(alice);
        bought(bob);

        reviewService.create(alice.getId(), book.getId(), review(5));
        reviewService.create(bob.getId(), book.getId(), review(2));
        em.flush();
        em.clear();

        Book reloaded = em.find(Book.class, book.getId());
        assertThat(reloaded.getTotalReviews()).isEqualTo(2);
        assertThat(reloaded.getAverageRating()).isEqualTo(3.5);
    }

    @Test
    void averageIsRoundedToTwoDecimals() {
        User carol = em.persist(new User("Carol", "carol@example.com", "hash"));
        bought(alice);
        bought(bob);
        bought(carol);

        reviewService.create(alice.getId(), book.getId(), review(5));
        reviewService.create(bob.getId(), book.getId(), review(5));
        reviewService.create(carol.getId(), book.getId(), review(4));

        assertThat(em.find(Book.class, book.getId()).getAverageRating()).isEqualTo(4.67);
    }

    @Test
    void customerWhoHasNotBoughtTheBookIsForbiddenAndNothingChanges() {
        assertThatThrownBy(() -> reviewService.create(alice.getId(), book.getId(), review(5)))
                .isInstanceOf(ForbiddenException.class);

        assertThat(reviewService.list(book.getId(), 0, 10).content()).isEmpty();
        assertThat(em.find(Book.class, book.getId()).getTotalReviews()).isZero();
    }

    @Test
    void secondReviewOfTheSameBookIsConflictAndKeepsTheFirst() {
        bought(alice);
        reviewService.create(alice.getId(), book.getId(), review(5));

        assertThatThrownBy(() -> reviewService.create(alice.getId(), book.getId(), review(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(reviewService.list(book.getId(), 0, 10).content()).hasSize(1);
        assertThat(em.find(Book.class, book.getId()).getAverageRating()).isEqualTo(5.0);
    }

    @Test
    void unknownBookIsNotFound() {
        assertThatThrownBy(() -> reviewService.create(alice.getId(), 9999L, review(5)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reviewService.list(9999L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listIsPagedAndShowsOnlyThatBooksReviews() {
        Category category = em.find(Category.class, book.getCategory().getId());
        Book other = em.persist(Book.builder("Other", "Writer", new BigDecimal("50.00"), category,
                book.getPublisher()).build());
        em.flush();
        bought(alice);
        bought(bob);
        purchases.purchases.add(alice.getId() + ":" + other.getId());
        reviewService.create(alice.getId(), book.getId(), review(5));
        reviewService.create(bob.getId(), book.getId(), review(3));
        reviewService.create(alice.getId(), other.getId(), review(1));
        em.flush();
        em.clear();

        PageResponse<ReviewResponse> firstPage = reviewService.list(book.getId(), 0, 1);
        PageResponse<ReviewResponse> all = reviewService.list(book.getId(), 0, 10);

        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(all.content()).extracting(ReviewResponse::bookId).containsOnly(book.getId());
        assertThat(all.content()).extracting(ReviewResponse::reviewerName).containsExactlyInAnyOrder("Alice Smith", "Bob Jones");
    }

    @Test
    void commentIsTrimmed() {
        bought(alice);

        ReviewResponse response = reviewService.create(alice.getId(), book.getId(),
                new ReviewRequest(4, "   Padded with whitespace.   "));

        assertThat(response.comment()).isEqualTo("Padded with whitespace.");
    }
}
