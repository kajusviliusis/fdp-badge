package com.bookworm.book;

import java.math.BigDecimal;

import com.bookworm.category.CategoryResponse;
import com.bookworm.publisher.PublisherResponse;

/**
 * Full book representation: all {@link BookSummaryResponse} fields plus the detail-only ones.
 */
public record BookDetailResponse(
        Long id,
        String title,
        String author,
        BigDecimal price,
        BookFormat format,
        String coverImageUrl,
        CategoryResponse category,
        int estimatedDeliveryDays,
        boolean inStock,
        String description,
        PublisherResponse publisher,
        String language,
        String isbn,
        int stockQuantity,
        double averageRating,
        int totalReviews,
        int copiesSold) {

    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getFormat(),
                book.getCoverImageUrl(),
                CategoryResponse.from(book.getCategory()),
                book.getEstimatedDeliveryDays(),
                book.isInStock(),
                book.getDescription(),
                PublisherResponse.from(book.getPublisher()),
                book.getLanguage(),
                book.getIsbn(),
                book.getStockQuantity(),
                book.getAverageRating(),
                book.getTotalReviews(),
                book.getCopiesSold());
    }
}
