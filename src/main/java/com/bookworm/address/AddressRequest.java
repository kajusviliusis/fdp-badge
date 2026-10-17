package com.bookworm.address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(min = 5, max = 255) String street,
        @NotBlank @Size(min = 2, max = 100) String city,
        @Size(max = 100) String state,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit postal code") String postalCode,
        @NotBlank @Size(min = 2, max = 100) String country,
        @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "must be a valid phone number") String phone,
        @Email @Size(max = 255) String email) {
}
