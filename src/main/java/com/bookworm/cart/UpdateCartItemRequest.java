package com.bookworm.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull @Min(1) @Max(Cart.MAX_QUANTITY_PER_BOOK) Integer quantity) {
}
