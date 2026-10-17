package com.bookworm.recommendation;

/**
 * A book the user has ordered, reduced to the attributes that reveal their taste.
 */
public record PurchasedBook(Long bookId, Long categoryId, String author) {
}
