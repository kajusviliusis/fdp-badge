package com.bookworm.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * @param cardDetails required for {@link PaymentMethod#CREDIT_CARD} and {@link PaymentMethod#DEBIT_CARD}
 * @param upiDetails  required for {@link PaymentMethod#UPI}
 */
public record PaymentRequest(
        @NotNull PaymentMethod method,
        @Valid CardDetails cardDetails,
        @Valid UpiDetails upiDetails) {

    @JsonIgnore
    @AssertTrue(message = "cardDetails are required for card payments")
    public boolean isCardDetailsProvided() {
        return method == null || !method.isCard() || cardDetails != null;
    }

    @JsonIgnore
    @AssertTrue(message = "upiDetails are required for UPI payments")
    public boolean isUpiDetailsProvided() {
        return method != PaymentMethod.UPI || upiDetails != null;
    }
}
