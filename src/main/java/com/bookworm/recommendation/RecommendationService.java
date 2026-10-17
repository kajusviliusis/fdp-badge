package com.bookworm.recommendation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bookworm.book.Book;
import com.bookworm.book.BookRepository;
import com.bookworm.book.BookSummaryResponse;
import com.bookworm.order.OrderStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suggests books from a user's order history: first books by authors they have bought or in categories they have
 * bought from, then, if there is room left (or no history at all), what is selling best.
 * Books the user already ordered and books that are out of stock are never suggested.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    /** Placeholder so that "exclude nothing" never produces an empty {@code NOT IN ()} list. */
    private static final Long NO_BOOK = -1L;

    private final PurchaseHistoryRepository purchaseHistory;
    private final BookRepository bookRepository;

    public RecommendationService(PurchaseHistoryRepository purchaseHistory, BookRepository bookRepository) {
        this.purchaseHistory = purchaseHistory;
        this.bookRepository = bookRepository;
    }

    public List<BookSummaryResponse> recommendFor(Long userId, int limit) {
        List<PurchasedBook> history = purchaseHistory.findBooksOrderedBy(userId, OrderStatus.CANCELLED);
        Set<Long> excluded = history.stream().map(PurchasedBook::bookId).collect(Collectors.toCollection(HashSet::new));
        excluded.add(NO_BOOK);

        List<Book> picks = new ArrayList<>(matchingTaste(history, excluded, limit));
        if (picks.size() < limit) {
            picks.forEach(book -> excluded.add(book.getId()));
            picks.addAll(bookRepository.findPopularExcluding(excluded, PageRequest.of(0, limit - picks.size())));
        }
        return picks.stream().map(BookSummaryResponse::from).toList();
    }

    private List<Book> matchingTaste(List<PurchasedBook> history, Set<Long> excluded, int limit) {
        if (history.isEmpty()) {
            return List.of();
        }
        Set<Long> categories = history.stream().map(PurchasedBook::categoryId).collect(Collectors.toSet());
        Set<String> authors = history.stream().map(PurchasedBook::author).collect(Collectors.toSet());
        return bookRepository.findLikedBy(excluded, categories, authors, PageRequest.of(0, limit));
    }
}
