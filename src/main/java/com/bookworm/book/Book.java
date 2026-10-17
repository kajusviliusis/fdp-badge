package com.bookworm.book;

import java.math.BigDecimal;
import java.time.Instant;

import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.publisher.Publisher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;

/**
 * A catalog entry. {@code copiesSold}, {@code averageRating} and {@code totalReviews} are denormalised
 * counters so that list queries can sort on them without aggregating orders or reviews.
 */
@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_category", columnList = "category_id"),
        @Index(name = "idx_books_publisher", columnList = "publisher_id"),
        @Index(name = "idx_books_created_at", columnList = "created_at")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "estimated_delivery_days", nullable = false)
    private int estimatedDeliveryDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookFormat format;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "total_reviews", nullable = false)
    private int totalReviews;

    @Column(name = "copies_sold", nullable = false)
    private int copiesSold;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Guards stock against lost updates when concurrent orders compete for the last copies. */
    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    protected Book() {
    }

    private Book(Builder builder) {
        this.title = builder.title;
        this.author = builder.author;
        this.description = builder.description;
        this.price = builder.price;
        this.stockQuantity = builder.stockQuantity;
        this.estimatedDeliveryDays = builder.estimatedDeliveryDays;
        this.format = builder.format;
        this.language = builder.language;
        this.isbn = builder.isbn;
        this.coverImageUrl = builder.coverImageUrl;
        this.copiesSold = builder.copiesSold;
        this.category = builder.category;
        this.publisher = builder.publisher;
    }

    public static Builder builder(String title, String author, BigDecimal price, Category category,
                                  Publisher publisher) {
        return new Builder(title, author, price, category, publisher);
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /**
     * Takes copies out of stock for an order.
     *
     * @throws BusinessRuleException if fewer than {@code quantity} copies are available
     */
    public void reserveStock(int quantity) {
        if (quantity > stockQuantity) {
            throw new BusinessRuleException(
                    "Only %d copies of '%s' are in stock.".formatted(stockQuantity, title));
        }
        stockQuantity -= quantity;
    }

    /** Returns copies to stock, e.g. when an order is cancelled. */
    public void restock(int quantity) {
        stockQuantity += quantity;
    }

    /** Counts copies towards the book's sales once their order is paid. */
    public void recordSale(int quantity) {
        copiesSold += quantity;
    }

    /** Reverses {@link #recordSale(int)} when a paid order is cancelled. */
    public void revokeSale(int quantity) {
        copiesSold = Math.max(0, copiesSold - quantity);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getEstimatedDeliveryDays() {
        return estimatedDeliveryDays;
    }

    public BookFormat getFormat() {
        return format;
    }

    public String getLanguage() {
        return language;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getCopiesSold() {
        return copiesSold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Category getCategory() {
        return category;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    /**
     * Builder with the mandatory attributes as constructor arguments and sensible defaults for the rest.
     */
    public static final class Builder {

        private final String title;
        private final String author;
        private final BigDecimal price;
        private final Category category;
        private final Publisher publisher;
        private String description;
        private int stockQuantity;
        private int estimatedDeliveryDays = 3;
        private BookFormat format = BookFormat.PAPERBACK;
        private String language = "English";
        private String isbn;
        private String coverImageUrl;
        private int copiesSold;

        private Builder(String title, String author, BigDecimal price, Category category, Publisher publisher) {
            this.title = title;
            this.author = author;
            this.price = price;
            this.category = category;
            this.publisher = publisher;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder stockQuantity(int stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }

        public Builder estimatedDeliveryDays(int estimatedDeliveryDays) {
            this.estimatedDeliveryDays = estimatedDeliveryDays;
            return this;
        }

        public Builder format(BookFormat format) {
            this.format = format;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public Builder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public Builder copiesSold(int copiesSold) {
            this.copiesSold = copiesSold;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}
