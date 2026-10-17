package com.bookworm.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.bookworm.book.Book;
import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.publisher.Publisher;
import com.bookworm.user.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CartTest {

    private final Cart cart = new Cart(new User("Arjun", "a@example.com", "hash"));

    @Test
    void addingSameBookTwiceIncrementsQuantity() {
        Book book = book(1L, "100.00", 10);

        cart.addBook(book, 2);
        cart.addBook(book, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addingBeyondStockIsRejectedAndLeavesCartUnchanged() {
        Book book = book(1L, "100.00", 3);
        cart.addBook(book, 2);

        assertThatThrownBy(() -> cart.addBook(book, 2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 3 copies");
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addingOutOfStockBookIsRejected() {
        assertThatThrownBy(() -> cart.addBook(book(1L, "100.00", 0), 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void quantityPerBookIsCappedAtNinetyNine() {
        Book book = book(1L, "10.00", 500);
        cart.addBook(book, 99);

        assertThatThrownBy(() -> cart.addBook(book, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("99");
    }

    @Test
    void changeQuantityReplacesQuantity() {
        cart.addBook(book(1L, "100.00", 10), 5);

        cart.changeQuantity(1L, 2);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void changeQuantityBeyondStockIsRejected() {
        cart.addBook(book(1L, "100.00", 4), 1);

        assertThatThrownBy(() -> cart.changeQuantity(1L, 5)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changeQuantityOfAbsentBookIsNotFound() {
        assertThatThrownBy(() -> cart.changeQuantity(9L, 1)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeBookDeletesOnlyThatLine() {
        cart.addBook(book(1L, "100.00", 10), 1);
        cart.addBook(book(2L, "50.00", 10), 1);

        cart.removeBook(1L);

        assertThat(cart.getItems()).extracting(item -> item.getBook().getId()).containsExactly(2L);
    }

    @Test
    void removeAbsentBookIsNotFound() {
        assertThatThrownBy(() -> cart.removeBook(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void subtotalSumsLineTotals() {
        cart.addBook(book(1L, "149.00", 10), 2);
        cart.addBook(book(2L, "210.00", 10), 1);

        assertThat(cart.subtotal()).isEqualByComparingTo("508.00");
    }

    @Test
    void clearEmptiesCartAndSubtotalIsZero() {
        cart.addBook(book(1L, "149.00", 10), 2);

        cart.clear();

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void addAvailableAddsOnlyWhatStockAllows() {
        Book book = book(1L, "10.00", 3);

        assertThat(cart.addAvailable(book, 5)).isEqualTo(3);
        assertThat(cart.addAvailable(book, 2)).isZero();
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void addAvailableRespectsPerBookLimit() {
        Book book = book(1L, "10.00", 500);
        cart.addBook(book, 95);

        assertThat(cart.addAvailable(book, 10)).isEqualTo(4);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(99);
    }

    @Test
    void addAvailableSkipsOutOfStockBook() {
        assertThat(cart.addAvailable(book(1L, "10.00", 0), 2)).isZero();
        assertThat(cart.getItems()).isEmpty();
    }

    private static Book book(Long id, String price, int stock) {
        Book book = Book.builder("Title " + id, "Author", new BigDecimal(price),
                new Category("Fiction"), new Publisher("Publisher")).stockQuantity(stock).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
