package com.bookworm.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.bookworm.category.Category;
import com.bookworm.publisher.Publisher;
import com.bookworm.support.PostgresDataJpaTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Proves the Flyway migrations produce a schema Hibernate accepts ({@code ddl-auto=validate}, checked while the
 * context starts) and that PostgreSQL enforces the constraints the application relies on.
 */
@PostgresDataJpaTest
class PostgresSchemaTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager em;

    private Category category;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        category = em.persist(new Category("Science Fiction"));
        publisher = em.persist(new Publisher("Chilton Books"));
    }

    @Test
    void persistsAndReadsBookOnPostgres() {
        Book saved = bookRepository.saveAndFlush(newBook("Dune", "9780441013593"));
        em.clear();

        Book loaded = bookRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getTitle()).isEqualTo("Dune");
        assertThat(loaded.getPrice()).isEqualByComparingTo("499.00");
    }

    @Test
    void rejectsDuplicateIsbn() {
        bookRepository.saveAndFlush(newBook("First", "9780000000001"));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(newBook("Second", "9780000000001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Book newBook(String title, String isbn) {
        return Book.builder(title, "Frank Herbert", new BigDecimal("499.00"), category, publisher)
                .isbn(isbn).stockQuantity(3).build();
    }
}
