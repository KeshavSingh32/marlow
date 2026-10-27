package com.marlow.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateReviewRequest(
        @NotBlank String text,
        String author,
        @Min(1) @Max(5) Integer rating,
        String source,
        Instant reviewedAt,
        String tone
) {
}
