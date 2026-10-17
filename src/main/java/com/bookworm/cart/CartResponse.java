package com.bookworm.cart;

import java.math.BigDecimal;
import java.util.List;

import com.bookworm.pricing.PriceBreakdown;

/**
 * @param totalItems total number of copies across all lines
 */
public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal deliveryCharges,
        int totalItems) {

    static CartResponse from(Cart cart, PriceBreakdown prices) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getId(), items, prices.subtotal(), prices.tax(), prices.deliveryCharges(),
                totalItems);
    }
}
