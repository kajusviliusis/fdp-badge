package com.bookworm.book;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Sort NEWEST_ORDER = BookSortOption.NEWEST.toSort();

    private final BookRepository bookRepository;
    private final SalesReport salesReport;
    private final Clock clock;

    public BookService(BookRepository bookRepository, SalesReport salesReport, Clock clock) {
        this.bookRepository = bookRepository;
        this.salesReport = salesReport;
        this.clock = clock;
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
     * Books ranked by copies sold in the current calendar month (UTC). Only paid, non-cancelled orders count, so
     * early in a month, or in a quiet one, the list may be shorter than {@code limit} or empty.
     */
    public List<BookSummaryResponse> findBestsellers(int limit) {
        YearMonth month = YearMonth.now(clock);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant until = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Long> rankedIds = salesReport.topSellingBookIds(from, until, limit);
        if (rankedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Book> booksById = bookRepository.findWithCategoryByIdIn(rankedIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return rankedIds.stream().map(booksById::get).map(BookSummaryResponse::from).toList();
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
