package com.bookworm.publisher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublisherRequest(@NotBlank @Size(min = 2, max = 200) String name) {
}
