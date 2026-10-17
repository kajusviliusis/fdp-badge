package com.bookworm.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookId,
        Long userId,
        String reviewerName,
        int rating,
        String comment,
        Instant createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getBook().getId(), review.getUser().getId(),
                review.getUser().getName(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
