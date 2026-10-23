package com.bookworm.book;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Query parameters accepted by {@code GET /books}. Missing paging and sorting values default to
 * {@code page=0}, {@code size=20} and {@code relevance}.
 */
public record BookSearchCriteria(
        String q,
        Long categoryId,
        Long publisherId,
        String language,
        BookFormat format,
        @DecimalMin("0") BigDecimal minPrice,
        @DecimalMin("0") BigDecimal maxPrice,
        BookSortOption sortBy,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size) {

    private static final int DEFAULT_PAGE_SIZE = 20;

    public BookSearchCriteria {
        q = normalise(q);
        language = normalise(language);
        sortBy = sortBy == null ? BookSortOption.RELEVANCE : sortBy;
        page = page == null ? 0 : page;
        size = size == null ? DEFAULT_PAGE_SIZE : size;
    }

    /**
     * Whether results are ordered by full-text relevance: a text query sorted by {@code relevance}. Without a text
     * query there is nothing to rank by, so {@code relevance} falls back to popularity.
     */
    public boolean rankedByText() {
        return q != null && sortBy == BookSortOption.RELEVANCE;
    }

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    private static String normalise(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
