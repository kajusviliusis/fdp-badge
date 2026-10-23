package com.bookworm.book;

import org.springframework.data.domain.Sort;

/**
 * Sort orders accepted by {@code GET /books}. Every order ends with an {@code id} tie-breaker so that
 * pagination is deterministic.
 */
public enum BookSortOption {

    /** Ranked by text relevance when a query is given (see {@link BookSpecifications}); otherwise by popularity. */
    RELEVANCE("relevance", Sort.by(Sort.Order.desc("copiesSold"), Sort.Order.asc("id"))),
    PRICE_ASC("price_asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"))),
    PRICE_DESC("price_desc", Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"))),
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    BESTSELLER("bestseller", Sort.by(Sort.Order.desc("copiesSold"), Sort.Order.asc("id")));

    private final String value;
    private final Sort sort;

    BookSortOption(String value, Sort sort) {
        this.value = value;
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    /**
     * Resolves the wire value (e.g. {@code price_asc}) to an option.
     *
     * @throws IllegalArgumentException if the value is not a supported sort order
     */
    public static BookSortOption fromValue(String value) {
        for (BookSortOption option : values()) {
            if (option.value.equalsIgnoreCase(value)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Unsupported sort order: " + value);
    }
}
