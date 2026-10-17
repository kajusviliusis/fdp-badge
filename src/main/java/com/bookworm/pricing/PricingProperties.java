package com.bookworm.pricing;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tunable pricing rules, bound from {@code bookworm.pricing.*}.
 *
 * @param taxRate               fraction of the subtotal charged as tax (0.12 = 12%)
 * @param freeDeliveryThreshold subtotal at or above which delivery is free
 * @param deliveryCharge        flat delivery fee below the threshold
 */
@ConfigurationProperties(prefix = "bookworm.pricing")
public record PricingProperties(
        @DefaultValue("0.12") BigDecimal taxRate,
        @DefaultValue("500.00") BigDecimal freeDeliveryThreshold,
        @DefaultValue("40.00") BigDecimal deliveryCharge) {
}
