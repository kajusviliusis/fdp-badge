package com.bookworm.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** @param code the normalised (upper-case) code, see {@link Coupon#normalizeCode(String)} */
    Optional<Coupon> findByCode(String code);
}
