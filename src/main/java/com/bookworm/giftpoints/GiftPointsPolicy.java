package com.bookworm.giftpoints;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.bookworm.common.error.BusinessRuleException;

import org.springframework.stereotype.Service;

/**
 * Rules for earning and redeeming gift points.
 */
@Service
public class GiftPointsPolicy {

    private static final int MONEY_SCALE = 2;

    private final GiftPointsProperties properties;

    public GiftPointsPolicy(GiftPointsProperties properties) {
        this.properties = properties;
    }

    /**
     * Whole points earned for paying {@code amountPaid}; any remainder below one point is discarded.
     */
    public int pointsEarnedFor(BigDecimal amountPaid) {
        return amountPaid
                .divide(BigDecimal.valueOf(properties.rupeesPerPoint()), 0, RoundingMode.DOWN)
                .max(BigDecimal.ZERO)
                .intValueExact();
    }

    /** Redeeming is fixed at 1 point = ₹1 off. */
    public BigDecimal discountFor(int pointsRedeemed) {
        return BigDecimal.valueOf(pointsRedeemed).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Points can only reduce the subtotal; tax and delivery are always paid in full.
     *
     * @throws BusinessRuleException if the points would be worth more than the subtotal
     */
    public void requireWithinSubtotal(int pointsRedeemed, BigDecimal subtotal) {
        if (discountFor(pointsRedeemed).compareTo(subtotal) > 0) {
            throw new BusinessRuleException(
                    "Gift points to redeem (%d) cannot exceed the order subtotal (%s).".formatted(pointsRedeemed, subtotal));
        }
    }
}
