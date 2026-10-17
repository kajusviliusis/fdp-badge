package com.bookworm.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    @Query("select avg(r.rating) as average, count(r) as total from Review r where r.book.id = :bookId")
    RatingSummary summarize(@Param("bookId") Long bookId);
}
