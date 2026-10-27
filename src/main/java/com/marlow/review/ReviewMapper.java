package com.marlow.review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class ReviewMapper {

    private static final TypeReference<List<String>> THEMES = new TypeReference<>() {
    };

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review, ObjectMapper objectMapper) {
        return new ReviewResponse(
                review.getId(),
                review.getAuthorName(),
                review.getRating(),
                review.getSource(),
                review.getBody(),
                review.getReviewedAt(),
                review.getSentiment().name(),
                review.getSentimentScore(),
                review.getCategory().name(),
                readThemes(review.getThemes(), objectMapper),
                review.getDraftReply(),
                review.getReplyStatus().name(),
                review.getModelName(),
                review.getCreatedAt()
        );
    }

    public static String writeThemes(List<String> themes, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(themes == null ? List.of() : themes);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private static List<String> readThemes(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, THEMES);
        } catch (Exception exception) {
            return List.of(raw);
        }
    }
}
