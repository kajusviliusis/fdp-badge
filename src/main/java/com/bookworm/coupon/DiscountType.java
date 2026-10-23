package com.bookworm.coupon;

public enum DiscountType {
    /** A fixed rupee amount off the order subtotal. */
    FIXED,
    /** A percentage of the order subtotal, optionally capped by {@link Coupon#getMaxDiscount()}. */
    PERCENT
}
