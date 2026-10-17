package com.bookworm.order;

import java.math.BigDecimal;

import com.bookworm.book.BookSummaryResponse;

public record OrderItemResponse(Long id, BookSummaryResponse book, int quantity, BigDecimal unitPrice,
                                BigDecimal lineTotal) {

    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), BookSummaryResponse.from(item.getBook()), item.getQuantity(),
                item.getUnitPrice(), item.lineTotal());
    }
}
