package com.bookworm.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpiDetails(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$", message = "must be a valid UPI ID")
        String upiId) {

    @Override
    public String toString() {
        return "UpiDetails[redacted]";
    }
}
