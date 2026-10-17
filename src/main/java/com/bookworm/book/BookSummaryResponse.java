package com.bookworm.book;

import java.math.BigDecimal;

import com.bookworm.category.CategoryResponse;

/**
 * Lightweight book representation used in lists and carousels.
 */
public record BookSummaryResponse(
        Long id,
        String title,
        String author,
        BigDecimal price,
        BookFormat format,
        String coverImageUrl,
        CategoryResponse category,
        int estimatedDeliveryDays,
        boolean inStock) {

    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getFormat(),
                book.getCoverImageUrl(),
                CategoryResponse.from(book.getCategory()),
                book.getEstimatedDeliveryDays(),
                book.isInStock());
    }
}
