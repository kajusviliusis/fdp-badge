package com.bookworm.cart;

import java.math.BigDecimal;

import com.bookworm.book.BookSummaryResponse;

public record CartItemResponse(Long id, BookSummaryResponse book, int quantity, BigDecimal lineTotal) {

    static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.getId(), BookSummaryResponse.from(item.getBook()),
                item.getQuantity(), item.lineTotal());
    }
}
