package com.marlow.intelligence;

import com.marlow.review.ReviewCategory;
import com.marlow.review.Sentiment;

import java.util.List;

public record ReviewInsight(
        Sentiment sentiment,
        double score,
        ReviewCategory category,
        List<String> themes,
        String draftReply,
        String model
) {
}
