package com.marlow.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        String author,
        Integer rating,
        String source,
        String text,
        Instant reviewedAt,
        String sentiment,
        double score,
        String category,
        List<String> themes,
        String draftReply,
        String replyStatus,
        String model,
        Instant createdAt
) {
}
