package com.bookworm.review;

/**
 * Aggregate of all ratings given to one book.
 */
public interface RatingSummary {

    /** Mean rating, or {@code null} when the book has no reviews. */
    Double getAverage();

    long getTotal();
}
