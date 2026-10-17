package com.bookworm.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bookworm.book.Book;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A user's shopping basket. Owns the rules for what may be in it: at most {@value #MAX_QUANTITY_PER_BOOK}
 * copies of a book, and never more than is in stock.
 */
@Entity
@Table(name = "carts")
public class Cart {

    public static final int MAX_QUANTITY_PER_BOOK = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    /**
     * Adds {@code quantity} copies, increasing the quantity if the book is already in the cart.
     *
     * @throws BusinessRuleException if the resulting quantity exceeds the per-book limit or available stock
     */
    public void addBook(Book book, int quantity) {
        Optional<CartItem> existing = findItem(book.getId());
        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + quantity;
        requireAllowed(book, newQuantity);
        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
        } else {
            items.add(new CartItem(this, book, newQuantity));
        }
    }

    /**
     * Adds as many of the requested copies as limits and stock allow, without failing when some cannot be added.
     *
     * @return the number of copies actually added, possibly zero
     */
    public int addAvailable(Book book, int quantity) {
        int current = findItem(book.getId()).map(CartItem::getQuantity).orElse(0);
        int room = Math.min(MAX_QUANTITY_PER_BOOK, book.getStockQuantity()) - current;
        int toAdd = Math.min(quantity, room);
        if (toAdd <= 0) {
            return 0;
        }
        addBook(book, toAdd);
        return toAdd;
    }

    /**
     * Sets the quantity of a book that is already in the cart.
     *
     * @throws ResourceNotFoundException if the book is not in the cart
     * @throws BusinessRuleException     if the quantity exceeds the per-book limit or available stock
     */
    public void changeQuantity(Long bookId, int quantity) {
        CartItem item = requireItem(bookId);
        requireAllowed(item.getBook(), quantity);
        item.setQuantity(quantity);
    }

    /**
     * @throws ResourceNotFoundException if the book is not in the cart
     */
    public void removeBook(Long bookId) {
        items.remove(requireItem(bookId));
    }

    public void clear() {
        items.clear();
    }

    public Long getId() {
        return id;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    /** Sum of all line totals, before tax, delivery and discounts. */
    public BigDecimal subtotal() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItem(Long bookId) {
        return items.stream().filter(item -> item.isFor(bookId)).findFirst();
    }

    private CartItem requireItem(Long bookId) {
        return findItem(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id %s is not in the cart.".formatted(bookId)));
    }

    private static void requireAllowed(Book book, int quantity) {
        if (quantity > MAX_QUANTITY_PER_BOOK) {
            throw new BusinessRuleException(
                    "A cart can hold at most %d copies of a book.".formatted(MAX_QUANTITY_PER_BOOK));
        }
        if (quantity > book.getStockQuantity()) {
            throw new BusinessRuleException(
                    "Only %d copies of '%s' are in stock.".formatted(book.getStockQuantity(), book.getTitle()));
        }
    }
}
