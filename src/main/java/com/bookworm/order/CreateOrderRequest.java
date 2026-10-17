package com.bookworm.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param giftPointsToRedeem points to spend (1 point = ₹1); {@code null} means none
 * @param couponCode         accepted by the contract but no coupons exist yet, so any value is rejected
 */
public record CreateOrderRequest(
        @NotNull Long addressId,
        @Min(0) @Max(Integer.MAX_VALUE) Integer giftPointsToRedeem,
        @Size(max = 50) String couponCode) {

    int pointsToRedeem() {
        return giftPointsToRedeem == null ? 0 : giftPointsToRedeem;
    }

    boolean hasCoupon() {
        return couponCode != null && !couponCode.isBlank();
    }
}
