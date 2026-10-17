package com.bookworm.giftpoints;

import java.math.BigDecimal;

/**
 * What redeeming would do. {@code newOrderTotal} is the subtotal less the discount; tax and delivery are added at
 * checkout.
 */
public record GiftPointsRedemptionResponse(
        int pointsToRedeem,
        BigDecimal discountAmount,
        int remainingPoints,
        BigDecimal newOrderTotal) {
}
