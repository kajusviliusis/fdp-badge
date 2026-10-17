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

    /** Whether payment has been taken and the order has not been cancelled. */
    public boolean isPaid() {
        return this == CONFIRMED || this == SHIPPED || this == DELIVERED;
    }

    /** Whether the customer may still cancel from this state, subject to the cancellation window. */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
