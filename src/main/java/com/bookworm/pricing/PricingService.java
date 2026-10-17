package com.bookworm.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

/**
 * Single source of truth for tax and delivery rules, shared by the cart preview and order placement so
 * that the amount a customer sees in the cart is the amount they are charged.
 */
@Service
public class PricingService {

    private static final int SCALE = 2;

    private final PricingProperties properties;

    public PricingService(PricingProperties properties) {
        this.properties = properties;
    }

    /**
     * Tax is a percentage of the subtotal; delivery is a flat fee waived for large or empty baskets.
     */
    public PriceBreakdown calculate(BigDecimal subtotal) {
        BigDecimal normalised = subtotal.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = normalised.multiply(properties.taxRate()).setScale(SCALE, RoundingMode.HALF_UP);
        boolean deliveryApplies = normalised.signum() > 0
                && normalised.compareTo(properties.freeDeliveryThreshold()) < 0;
        BigDecimal delivery = (deliveryApplies ? properties.deliveryCharge() : BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);
        return new PriceBreakdown(normalised, tax, delivery);
    }
}
