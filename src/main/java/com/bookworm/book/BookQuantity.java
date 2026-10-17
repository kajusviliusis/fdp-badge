package com.bookworm.book;

import java.math.BigDecimal;

/**
 * A book together with a number of copies. Used to hand cart and order lines between features without
 * either depending on the other's entities.
 */
public record BookQuantity(Book book, int quantity) {

    /** Current list price multiplied by the quantity. */
    public BigDecimal lineTotal() {
        return book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
