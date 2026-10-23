package com.bookworm.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import com.bookworm.common.error.BusinessRuleException;

import org.junit.jupiter.api.Test;

class CouponTest {

    private static final Instant NOW = Instant.parse("2025-03-01T12:00:00Z");
    private static final BigDecimal SUBTOTAL = new BigDecimal("200.00");

    private static Coupon.Builder fixed(String value) {
        return Coupon.builder("SAVE", DiscountType.FIXED, new BigDecimal(value));
    }

    private static Coupon.Builder percent(String value) {
        return Coupon.builder("PCT", DiscountType.PERCENT, new BigDecimal(value));
    }

    @Test
    void codesAreNormalisedToUpperCase() {
        assertThat(Coupon.normalizeCode("  save100 ")).isEqualTo("SAVE100");
        assertThat(fixed("5").build().getCode()).isEqualTo("SAVE");
        assertThat(Coupon.builder("save100", DiscountType.FIXED, BigDecimal.TEN).build().getCode()).isEqualTo("SAVE100");
    }

    @Test
    void fixedDiscountIsCappedAtTheSubtotal() {
        assertThat(fixed("50").build().redeem(SUBTOTAL, NOW)).isEqualByComparingTo("50.00");
        assertThat(fixed("500").build().redeem(SUBTOTAL, NOW)).isEqualByComparingTo("200.00");
    }

    @Test
    void percentDiscountIsRoundedToPaiseAndCappedByMaxDiscount() {
        assertThat(percent("12.5").build().redeem(new BigDecimal("149.00"), NOW)).isEqualByComparingTo("18.63");
        assertThat(percent("50").maxDiscount(new BigDecimal("30")).build().redeem(SUBTOTAL, NOW))
                .isEqualByComparingTo("30.00");
        assertThat(percent("100").build().redeem(SUBTOTAL, NOW)).isEqualByComparingTo("200.00");
    }

    @Test
    void redeemingCountsAUseAndReleaseGivesItBack() {
        Coupon coupon = fixed("10").build();

        coupon.redeem(SUBTOTAL, NOW);
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
        coupon.release();
        coupon.release();

        assertThat(coupon.getTimesUsed()).isZero();
    }

    @Test
    void validityWindowStartIsInclusiveAndEndIsExclusive() {
        Coupon coupon = fixed("10").validFrom(NOW).validUntil(NOW.plusSeconds(60)).build();

        assertThat(coupon.redeem(SUBTOTAL, NOW)).isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> coupon.redeem(SUBTOTAL, NOW.minusSeconds(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("not valid yet");
        assertThatThrownBy(() -> coupon.redeem(SUBTOTAL, NOW.plusSeconds(60)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("expired");
    }

    @Test
    void minimumOrderAmountIsInclusive() {
        Coupon coupon = fixed("10").minOrderAmount(SUBTOTAL).build();

        assertThat(coupon.redeem(SUBTOTAL, NOW)).isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> coupon.redeem(new BigDecimal("199.99"), NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("minimum order of 200.00");
    }

    @Test
    void usageLimitIsEnforced() {
        Coupon coupon = fixed("10").usageLimit(2).build();
        coupon.redeem(SUBTOTAL, NOW);
        coupon.redeem(SUBTOTAL, NOW);

        assertThatThrownBy(() -> coupon.redeem(SUBTOTAL, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("usage limit");
    }

    @Test
    void inactiveCouponLooksUnknown() {
        Coupon coupon = fixed("10").active(false).build();

        assertThatThrownBy(() -> coupon.redeem(SUBTOTAL, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Coupon code 'SAVE' is not valid.");
    }

    @Test
    void failedRedemptionDoesNotUseUpACoupon() {
        Coupon coupon = fixed("10").minOrderAmount(SUBTOTAL).build();

        assertThatThrownBy(() -> coupon.redeem(BigDecimal.ONE, NOW)).isInstanceOf(BusinessRuleException.class);

        assertThat(coupon.getTimesUsed()).isZero();
    }
}
