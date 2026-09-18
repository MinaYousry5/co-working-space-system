package com.workspace.booking.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 4000, message = "Body must not exceed 4000 characters")
        String body
) {}
