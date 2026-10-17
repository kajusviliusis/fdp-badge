package com.bookworm.giftpoints;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param pointsToRedeem number of gift points the user is considering spending
 * @param orderSubtotal  the order's subtotal before any discount
 */
public record GiftPointsRedemptionRequest(
        @NotNull @Min(1) Integer pointsToRedeem,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal orderSubtotal) {
}
