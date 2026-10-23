package com.bookworm.coupon;

import java.math.BigDecimal;
import java.time.Instant;

import com.bookworm.common.error.BusinessRuleException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redeems promotional coupons at checkout.
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Validates the coupon against the order subtotal and uses up one redemption. Must run in the checkout
     * transaction: if the order fails afterwards the redemption is rolled back with it. Concurrent redemptions of
     * the last available use are resolved by the coupon's version, so only one of them can succeed.
     *
     * @param code     the code as typed by the customer; case and surrounding whitespace are ignored
     * @param subtotal the order subtotal before any discount
     * @throws BusinessRuleException if the code is unknown or the coupon cannot be applied to this order
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedCoupon redeem(String code, BigDecimal subtotal, Instant now) {
        Coupon coupon = couponRepository.findByCode(Coupon.normalizeCode(code))
                .orElseThrow(() -> Coupon.notValid(code));
        return new AppliedCoupon(coupon, coupon.redeem(subtotal, now));
    }
}
