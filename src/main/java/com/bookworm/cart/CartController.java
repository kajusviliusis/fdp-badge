package com.bookworm.cart;

import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@CurrentUserId Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@CurrentUserId Long userId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/items/{bookId}")
    public CartResponse updateItem(@CurrentUserId Long userId, @PathVariable Long bookId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId, bookId, request);
    }

    @DeleteMapping("/items/{bookId}")
    public CartResponse removeItem(@CurrentUserId Long userId, @PathVariable Long bookId) {
        return cartService.removeItem(userId, bookId);
    }

    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@CurrentUserId Long userId) {
        cartService.clear(userId);
    }
}
