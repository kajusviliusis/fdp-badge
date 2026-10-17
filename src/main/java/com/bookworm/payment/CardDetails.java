package com.bookworm.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Card details for a simulated payment. They are used for the single charge and never stored or logged;
 * {@link #toString()} is redacted so they cannot leak through accidental logging.
 */
public record CardDetails(
        @NotBlank @Pattern(regexp = "^\\d{16}$", message = "must be a 16-digit card number") String cardNumber,
        @NotBlank @Size(min = 2, max = 100) String nameOnCard,
        @NotBlank @Pattern(regexp = "^\\d{3,4}$", message = "must be a 3 or 4 digit CVV") String cvv,
        @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "must be in MM/YYYY format")
        String expiryDate) {

    @Override
    public String toString() {
        return "CardDetails[redacted]";
    }
}
