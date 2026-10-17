package com.bookworm.review;

import com.bookworm.book.Book;
import com.bookworm.book.BookRepository;
import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;
import com.bookworm.user.User;
import com.bookworm.user.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PurchaseVerifier purchaseVerifier;

    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository,
                         UserRepository userRepository, PurchaseVerifier purchaseVerifier) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.purchaseVerifier = purchaseVerifier;
    }

    /**
     * Lists a book's reviews, newest first.
     *
     * @throws ResourceNotFoundException if the book does not exist
     */
    public PageResponse<ReviewResponse> list(Long bookId, int page, int size) {
        requireBook(bookId);
        return PageResponse.of(
                reviewRepository.findByBookId(bookId, PageRequest.of(page, size, NEWEST_FIRST)),
                ReviewResponse::from);
    }

    /**
     * Records a review and refreshes the book's rating summary.
     *
     * @throws ResourceNotFoundException if the book does not exist
     * @throws ForbiddenException        if the user has not bought the book
     * @throws ConflictException         if the user has already reviewed the book
     */
    @Transactional
    public ReviewResponse create(Long userId, Long bookId, ReviewRequest request) {
        User user = userRepository.getAuthenticated(userId);
        Book book = requireBook(bookId);
        if (!purchaseVerifier.hasPurchased(userId, bookId)) {
            throw new ForbiddenException("Only customers who have bought this book can review it.");
        }
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new ConflictException("You have already reviewed this book.");
        }

        Review review = reviewRepository.saveAndFlush(new Review(book, user, request.rating(), request.comment().strip()));
        RatingSummary summary = reviewRepository.summarize(bookId);
        book.updateReviewSummary(summary.getAverage(), Math.toIntExact(summary.getTotal()));
        return ReviewResponse.from(review);
    }

    private Book requireBook(Long bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
    }
}
