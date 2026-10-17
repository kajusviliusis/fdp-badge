package com.bookworm.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(min = 10, max = 1000) String comment) {
}
