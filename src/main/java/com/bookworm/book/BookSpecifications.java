package com.bookworm.book;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the dynamic {@code WHERE} clause for catalog searches. Absent criteria produce no predicate.
 */
final class BookSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private BookSpecifications() {
    }

    static Specification<Book> matching(BookSearchCriteria criteria) {
        return Specification.allOf(
                titleOrAuthorContains(criteria.q()),
                inCategory(criteria.categoryId()),
                byPublisher(criteria.publisherId()),
                inLanguage(criteria.language()),
                hasFormat(criteria.format()),
                priceAtLeast(criteria.minPrice()),
                priceAtMost(criteria.maxPrice()));
    }

    private static Specification<Book> titleOrAuthorContains(String text) {
        if (text == null) {
            return null;
        }
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("author")), pattern, LIKE_ESCAPE));
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

    /** Prevents user-supplied {@code %} and {@code _} from acting as wildcards. */
    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
