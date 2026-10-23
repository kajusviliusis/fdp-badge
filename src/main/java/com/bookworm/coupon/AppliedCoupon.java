package com.bookworm.coupon;

import java.math.BigDecimal;

/**
 * A coupon that has been redeemed for an order, with the discount it gives.
 */
public record AppliedCoupon(Coupon coupon, BigDecimal discount) {
}
