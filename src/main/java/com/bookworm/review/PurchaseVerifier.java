package com.bookworm.review;

/**
 * Answers whether a customer has bought a book. Implemented by the order feature, which keeps this package free
 * of any dependency on how orders are stored.
 */
public interface PurchaseVerifier {

    /** Whether the user has a paid, non-cancelled order containing the book. */
    boolean hasPurchased(Long userId, Long bookId);
}
