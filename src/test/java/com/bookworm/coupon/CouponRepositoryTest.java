package com.bookworm.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.bookworm.support.PostgresDataJpaTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PostgresDataJpaTest
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void launchCouponsAreSeededByMigration() {
        Coupon save100 = couponRepository.findByCode("SAVE100").orElseThrow();
        Coupon welcome = couponRepository.findByCode("WELCOME10").orElseThrow();

        assertThat(save100.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(save100.getMinOrderAmount()).isEqualByComparingTo("500.00");
        assertThat(welcome.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(welcome.getMaxDiscount()).isEqualByComparingTo("150.00");
    }

    @Test
    void codesAreUniqueAndMustBeStoredUpperCase() {
        assertThatThrownBy(() -> couponRepository.saveAndFlush(
                Coupon.builder("SAVE100", DiscountType.FIXED, BigDecimal.TEN).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void percentageAboveHundredIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> couponRepository.saveAndFlush(
                Coupon.builder("TOOMUCH", DiscountType.PERCENT, new BigDecimal("101")).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
