package com.bookworm.book;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the dynamic {@code WHERE} clause for catalog searches. Absent criteria produce no predicate.
 *
 * <p>The text query uses PostgreSQL full-text search (see {@link FullTextSearchFunctionContributor}), so searches
 * match on stemmed words rather than substrings.
 */
final class BookSpecifications {

    private BookSpecifications() {
    }

    static Specification<Book> matching(BookSearchCriteria criteria) {
        return Specification.allOf(
                matchesText(criteria.q()),
                inCategory(criteria.categoryId()),
                byPublisher(criteria.publisherId()),
                inLanguage(criteria.language()),
                hasFormat(criteria.format()),
                priceAtLeast(criteria.minPrice()),
                priceAtMost(criteria.maxPrice()),
                rankedByTextRelevance(criteria));
    }

    private static Specification<Book> matchesText(String text) {
        if (text == null) {
            return null;
        }
        return (root, query, cb) -> cb.isTrue(cb.function(
                FullTextSearchFunctionContributor.MATCH, Boolean.class,
                root.get("title"), root.get("author"), cb.literal(text)));
    }

    /**
     * Orders by {@code ts_rank} (then popularity) when the criteria ask for relevance ordering of a text query.
     * Spring Data overwrites the query's {@code ORDER BY} with the {@code Pageable}'s sort when there is one, so
     * this only takes effect with an unsorted pageable (see {@link BookSearchCriteria#rankedByText()}).
     */
    private static Specification<Book> rankedByTextRelevance(BookSearchCriteria criteria) {
        if (!criteria.rankedByText()) {
            return null;
        }
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class) {
                Expression<Double> rank = cb.function(
                        FullTextSearchFunctionContributor.RANK, Double.class,
                        root.get("title"), root.get("author"), cb.literal(criteria.q()));
                List<Order> order = List.of(cb.desc(rank), cb.desc(root.get("copiesSold")), cb.asc(root.get("id")));
                query.orderBy(order);
            }
            return null;
        };
    }

    private static Specification<Book> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Book> byPublisher(Long publisherId) {
        return publisherId == null ? null : (root, query, cb) -> cb.equal(root.get("publisher").get("id"), publisherId);
    }

    private static Specification<Book> inLanguage(String language) {
        return language == null ? null
                : (root, query, cb) -> cb.equal(cb.lower(root.get("language")), language.toLowerCase(Locale.ROOT));
    }

    private static Specification<Book> hasFormat(BookFormat format) {
        return format == null ? null : (root, query, cb) -> cb.equal(root.get("format"), format);
    }

    private static Specification<Book> priceAtLeast(java.math.BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    private static Specification<Book> priceAtMost(java.math.BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

}
