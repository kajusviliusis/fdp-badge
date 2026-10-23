package com.bookworm.book;

import java.util.List;

import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Sort BESTSELLER_ORDER = BookSortOption.BESTSELLER.toSort();
    private static final Sort NEWEST_ORDER = BookSortOption.NEWEST.toSort();

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Searches the catalog. A text query matches title and author via full-text search; with {@code relevance}
     * sorting it is ordered by text rank, which the specification applies itself (hence the unsorted pageable).
     */
    public PageResponse<BookSummaryResponse> search(BookSearchCriteria criteria) {
        Sort sort = criteria.rankedByText() ? Sort.unsorted() : criteria.sortBy().toSort();
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);
        return PageResponse.of(
                bookRepository.findAll(BookSpecifications.matching(criteria), pageable),
                BookSummaryResponse::from);
    }

    public BookDetailResponse findById(Long id) {
        return bookRepository.findWithDetailsById(id)
                .map(BookDetailResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    /**
     * Ranks by lifetime copies sold; a monthly window requires order history and is added with orders.
     */
    public List<BookSummaryResponse> findBestsellers(int limit) {
        return toSummaries(bookRepository.findBy(PageRequest.of(0, limit, BESTSELLER_ORDER)));
    }

    public List<BookSummaryResponse> findNewLaunches(int limit) {
        return toSummaries(bookRepository.findBy(PageRequest.of(0, limit, NEWEST_ORDER)));
    }

    public List<BookSummaryResponse> findRelated(Long bookId, int limit) {
        Book book = bookRepository.findWithDetailsById(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        return toSummaries(bookRepository.findRelated(
                book.getId(), book.getCategory().getId(), book.getAuthor(), PageRequest.of(0, limit)));
    }

    private static List<BookSummaryResponse> toSummaries(List<Book> books) {
        return books.stream().map(BookSummaryResponse::from).toList();
    }
}
