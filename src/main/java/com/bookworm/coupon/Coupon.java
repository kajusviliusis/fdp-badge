package com.bookworm.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

import com.bookworm.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A promotional coupon: a fixed or percentage discount on the order subtotal, valid within an optional time window,
 * above an optional minimum subtotal and for a limited number of orders.
 */
@Entity
@Table(name = "coupons")
public class Coupon {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored upper-case, so that codes are matched case-insensitively. */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** Rupees for {@link DiscountType#FIXED}, a percentage (0-100] for {@link DiscountType#PERCENT}. */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** Upper limit for a percentage discount; {@code null} means uncapped. */
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    /** Total redemptions allowed; {@code null} means unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "times_used", nullable = false)
    private int timesUsed;

    @Column(nullable = false)
    private boolean active = true;

    /** Stops concurrent orders from redeeming the same last use twice. */
    @Version
    private long version;

    protected Coupon() {
    }

    private Coupon(Builder builder) {
        this.code = normalizeCode(builder.code);
        this.discountType = builder.discountType;
        this.discountValue = builder.discountValue.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        this.maxDiscount = builder.maxDiscount;
        this.minOrderAmount = builder.minOrderAmount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        this.validFrom = builder.validFrom;
        this.validUntil = builder.validUntil;
        this.usageLimit = builder.usageLimit;
        this.active = builder.active;
    }

    public static Builder builder(String code, DiscountType discountType, BigDecimal discountValue) {
        return new Builder(code, discountType, discountValue);
    }

    /** Codes are compared ignoring case and surrounding whitespace. */
    public static String normalizeCode(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * The same message for unknown and inactive coupons, so that the response does not reveal which codes exist.
     */
    static BusinessRuleException notValid(String code) {
        return new BusinessRuleException("Coupon code '%s' is not valid.".formatted(code.strip()));
    }

    /**
     * Checks that the coupon can be applied to an order and uses up one redemption.
     *
     * @return the discount in rupees, never more than the subtotal
     * @throws BusinessRuleException if the coupon is inactive, outside its validity window, used up, or the
     *                               subtotal is below its minimum
     */
    public BigDecimal redeem(BigDecimal subtotal, Instant now) {
        if (!active) {
            throw notValid(code);
        }
        if (validFrom != null && now.isBefore(validFrom)) {
            throw new BusinessRuleException("Coupon '%s' is not valid yet.".formatted(code));
        }
        if (validUntil != null && !now.isBefore(validUntil)) {
            throw new BusinessRuleException("Coupon '%s' has expired.".formatted(code));
        }
        if (usageLimit != null && timesUsed >= usageLimit) {
            throw new BusinessRuleException("Coupon '%s' has reached its usage limit.".formatted(code));
        }
        if (subtotal.compareTo(minOrderAmount) < 0) {
            throw new BusinessRuleException(
                    "Coupon '%s' requires a minimum order of %s.".formatted(code, minOrderAmount));
        }
        timesUsed++;
        return discountFor(subtotal);
    }

    /** Gives back a redemption, e.g. when the order it was used for is cancelled. */
    public void release() {
        if (timesUsed > 0) {
            timesUsed--;
        }
    }

    private BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal discount = switch (discountType) {
            case FIXED -> discountValue;
            case PERCENT -> {
                BigDecimal percentage = subtotal.multiply(discountValue).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
                yield maxDiscount == null ? percentage : percentage.min(maxDiscount);
            }
        };
        return discount.min(subtotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public boolean isActive() {
        return active;
    }

    public static final class Builder {

        private final String code;
        private final DiscountType discountType;
        private final BigDecimal discountValue;
        private BigDecimal maxDiscount;
        private BigDecimal minOrderAmount = BigDecimal.ZERO;
        private Instant validFrom;
        private Instant validUntil;
        private Integer usageLimit;
        private boolean active = true;

        private Builder(String code, DiscountType discountType, BigDecimal discountValue) {
            this.code = code;
            this.discountType = discountType;
            this.discountValue = discountValue;
        }

        public Builder maxDiscount(BigDecimal maxDiscount) {
            this.maxDiscount = maxDiscount;
            return this;
        }

        public Builder minOrderAmount(BigDecimal minOrderAmount) {
            this.minOrderAmount = minOrderAmount;
            return this;
        }

        public Builder validFrom(Instant validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder validUntil(Instant validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        public Builder usageLimit(Integer usageLimit) {
            this.usageLimit = usageLimit;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Coupon build() {
            return new Coupon(this);
        }
    }
}
