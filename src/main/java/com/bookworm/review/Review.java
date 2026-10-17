package com.bookworm.review;

import java.time.Instant;

import com.bookworm.book.Book;
import com.bookworm.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

/**
 * A customer's rating and comment on a book. A customer reviews each book at most once.
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_book_user", columnNames = {"book_id", "user_id"}),
        indexes = @Index(name = "idx_reviews_book_created", columnList = "book_id, created_at"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Review() {
    }

    Review(Book book, User user, int rating, String comment) {
        this.book = book;
        this.user = user;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public User getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
