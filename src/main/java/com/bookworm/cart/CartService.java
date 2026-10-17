package com.bookworm.cart;

import java.util.List;

import com.bookworm.book.Book;
import com.bookworm.book.BookQuantity;
import com.bookworm.book.BookRepository;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.pricing.PricingService;
import com.bookworm.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public CartService(CartRepository cartRepository, BookRepository bookRepository,
                       UserRepository userRepository, PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
    }

    /**
     * Returns the user's cart, which is created on first access so every user always has exactly one.
     */
    public CartResponse getCart(Long userId) {
        return toResponse(getOrCreateCart(userId));
    }

    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> ResourceNotFoundException.of("Book", request.bookId()));
        Cart cart = getOrCreateCart(userId);
        cart.addBook(book, request.quantity());
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    /**
     * Adds each line as far as stock and limits allow (used by "buy again").
     *
     * @throws BusinessRuleException if none of the lines could be added
     */
    public CartResponse addAvailableBooks(Long userId, List<BookQuantity> lines) {
        Cart cart = getOrCreateCart(userId);
        int added = lines.stream().mapToInt(line -> cart.addAvailable(line.book(), line.quantity())).sum();
        if (added == 0) {
            throw new BusinessRuleException("None of the requested books are currently available.");
        }
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    /**
     * Empties the user's cart and returns what was in it, for turning into an order. Runs in the caller's
     * transaction, so if order placement fails the cart is restored.
     */
    public List<BookQuantity> drain(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<BookQuantity> lines = cart.getItems().stream()
                .map(item -> new BookQuantity(item.getBook(), item.getQuantity()))
                .toList();
        cart.clear();
        return lines;
    }

    public CartResponse updateItem(Long userId, Long bookId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        cart.changeQuantity(bookId, request.quantity());
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    public CartResponse removeItem(Long userId, Long bookId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeBook(bookId);
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    public void clear(Long userId) {
        getOrCreateCart(userId).clear();
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        return cartRepository.save(new Cart(userRepository.getAuthenticated(userId)));
    }

    private CartResponse toResponse(Cart cart) {
        return CartResponse.from(cart, pricingService.calculate(cart.subtotal()));
    }
}
