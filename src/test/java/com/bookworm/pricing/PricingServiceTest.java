package com.bookworm.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private final PricingService pricing = new PricingService(
            new PricingProperties(new BigDecimal("0.12"), new BigDecimal("500.00"), new BigDecimal("40.00")));

    @Test
    void chargesDeliveryBelowThreshold() {
        PriceBreakdown result = pricing.calculate(new BigDecimal("100.00"));

        assertThat(result.tax()).isEqualByComparingTo("12.00");
        assertThat(result.deliveryCharges()).isEqualByComparingTo("40.00");
        assertThat(result.total()).isEqualByComparingTo("152.00");
    }

    @Test
    void deliveryIsFreeAtThreshold() {
        PriceBreakdown result = pricing.calculate(new BigDecimal("500.00"));

        assertThat(result.deliveryCharges()).isEqualByComparingTo("0.00");
        assertThat(result.tax()).isEqualByComparingTo("60.00");
    }

    @Test
    void emptyBasketCostsNothing() {
        PriceBreakdown result = pricing.calculate(BigDecimal.ZERO);

        assertThat(result.total()).isEqualByComparingTo("0.00");
    }

    @Test
    void taxIsRoundedHalfUpToTwoDecimals() {
        PriceBreakdown result = pricing.calculate(new BigDecimal("149.99"));

        assertThat(result.tax()).isEqualByComparingTo("18.00");
        assertThat(result.tax().scale()).isEqualTo(2);
    }
}
