package com.bookworm.order;

/**
 * Lifecycle of an order. Transitions beyond {@link #CANCELLED} are driven by payment and fulfilment.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /** Whether the customer may still cancel from this state, subject to the cancellation window. */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
