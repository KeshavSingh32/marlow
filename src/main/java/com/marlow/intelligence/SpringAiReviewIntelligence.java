package com.marlow.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlow.review.ReviewCategory;
import com.marlow.review.Sentiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Locale;

public class SpringAiReviewIntelligence implements ReviewIntelligence {

    private static final Logger log = LoggerFactory.getLogger(SpringAiReviewIntelligence.class);

    private static final String SYSTEM = """
            You write for a neighborhood shop's review desk.
            Read one customer review and return JSON only, with no markdown.
            Schema:
            {
              "sentiment": "POSITIVE" or "NEUTRAL" or "NEGATIVE",
              "score": number from -1.0 (very unhappy) to 1.0 (very happy),
              "category": one of PRODUCT, SERVICE, DELIVERY, PRICING, SUPPORT, AMBIENCE, OTHER,
              "themes": array of up to 3 short phrases taken from what they actually said,
              "draftReply": string
            }
            The draft reply is polite, specific, and under 80 words.
            Use the requested tone. Do not invent refunds, discounts, or facts that are not in the review.
            Do not mention that you are an AI.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ReviewIntelligence fallback;
    private final String model;
    private final long minIntervalMillis;
    private long nextCallAt;

    public SpringAiReviewIntelligence(ChatClient chatClient, ObjectMapper objectMapper,
                                      ReviewIntelligence fallback, String model, long minIntervalMillis) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
        this.model = model;
        this.minIntervalMillis = minIntervalMillis;
    }

    @Override
    public String mode() {
        return "openai";
    }

    @Override
    public ReviewInsight analyze(AnalysisInput input) {
        try {
            awaitTurn();
            String raw = chatClient.prompt()
                    .system(SYSTEM)
                    .user(userPrompt(input))
                    .call()
                    .content();
            ModelReply reply = objectMapper.readValue(jsonOnly(raw), ModelReply.class);
            return map(reply, input);
        } catch (Exception exception) {
            log.warn("Spring AI call failed, using the on-desk reading instead: {}", exception.getMessage());
            ReviewInsight local = fallback.analyze(input);
            return new ReviewInsight(local.sentiment(), local.score(), local.category(), local.themes(),
                    local.draftReply(), "heuristic-fallback");
        }
    }

    private synchronized void awaitTurn() throws InterruptedException {
        long wait = nextCallAt - System.currentTimeMillis();
        if (wait > 0) {
            Thread.sleep(wait);
        }
        nextCallAt = System.currentTimeMillis() + minIntervalMillis;
    }

    private ReviewInsight map(ModelReply reply, AnalysisInput input) {
        Sentiment sentiment = parseSentiment(reply.sentiment());
        ReviewCategory category = parseCategory(reply.category());
        double score = reply.score() == null ? 0 : Math.clamp(reply.score(), -1.0, 1.0);
        List<String> themes = reply.themes() == null ? List.of() : reply.themes().stream().limit(3).toList();
        String draft = reply.draftReply();
        if (draft == null || draft.isBlank()) {
            draft = fallback.analyze(input).draftReply();
        }
        if (sentiment == null) {
            sentiment = score >= 0.18 ? Sentiment.POSITIVE : score <= -0.18 ? Sentiment.NEGATIVE : Sentiment.NEUTRAL;
        }
        if (category == null) {
            category = ReviewCategory.OTHER;
        }
        return new ReviewInsight(sentiment, Math.round(score * 100.0) / 100.0, category, themes, draft.trim(), model);
    }

    private static String userPrompt(AnalysisInput input) {
        return """
                Shop: %s
                Trade: %s
                Tone: %s
                Author: %s
                Stars out of 5: %s
                Source: %s
                Review:
                %s
                """.formatted(
                nullTo(input.businessName(), "the shop"),
                nullTo(input.industry(), "local business"),
                nullTo(input.tone(), "warm"),
                nullTo(input.author(), "a customer"),
                input.rating() == null ? "unrated" : input.rating().toString(),
                nullTo(input.source(), "unknown"),
                input.text()
        );
    }

    public static String jsonOnly(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        int brace = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (brace >= 0 && last > brace) {
            return trimmed.substring(brace, last + 1);
        }
        return trimmed;
    }

    private static Sentiment parseSentiment(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Sentiment.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ReviewCategory parseCategory(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ReviewCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String nullTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
