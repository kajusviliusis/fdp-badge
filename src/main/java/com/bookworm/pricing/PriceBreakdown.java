package com.bookworm.pricing;

import java.math.BigDecimal;

/**
 * Amounts derived from a subtotal. All values have a scale of 2.
 */
public record PriceBreakdown(BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryCharges) {

    /** Subtotal plus tax and delivery, before any discount. */
    public BigDecimal total() {
        return subtotal.add(tax).add(deliveryCharges);
    }

    /** The amount payable once {@code discount} has been taken off the {@linkplain #total() total}. */
    public BigDecimal totalAfter(BigDecimal discount) {
        return total().subtract(discount);
    }
}
