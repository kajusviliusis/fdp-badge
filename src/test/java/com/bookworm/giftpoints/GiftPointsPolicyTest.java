package com.bookworm.giftpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GiftPointsPolicyTest {

    private final GiftPointsPolicy policy = new GiftPointsPolicy(new GiftPointsProperties(10));

    @Test
    void earnsOnePointPerTenRupeesRoundedDown() {
        assertThat(policy.pointsEarnedFor(new BigDecimal("470.00"))).isEqualTo(47);
        assertThat(policy.pointsEarnedFor(new BigDecimal("479.99"))).isEqualTo(47);
        assertThat(policy.pointsEarnedFor(new BigDecimal("9.99"))).isZero();
    }

    @Test
    void earnsNothingForZeroOrNegativeAmounts() {
        assertThat(policy.pointsEarnedFor(BigDecimal.ZERO)).isZero();
        assertThat(policy.pointsEarnedFor(new BigDecimal("-50.00"))).isZero();
    }

    @Test
    void redeemingIsOneRupeePerPoint() {
        assertThat(policy.discountFor(50)).isEqualByComparingTo("50.00");
        assertThat(policy.discountFor(0)).isEqualByComparingTo("0.00");
        assertThat(policy.discountFor(50).scale()).isEqualTo(2);
    }
}
