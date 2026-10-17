package com.bookworm.cart;

import com.bookworm.book.Book;
import com.bookworm.book.BookRepository;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.error.UnauthorizedException;
import com.bookworm.pricing.PricingService;
import com.bookworm.user.User;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
        return cartRepository.save(new Cart(user));
    }

    private CartResponse toResponse(Cart cart) {
        return CartResponse.from(cart, pricingService.calculate(cart.subtotal()));
    }
}
