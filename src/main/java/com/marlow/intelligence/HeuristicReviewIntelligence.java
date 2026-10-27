package com.marlow.intelligence;

import com.marlow.review.ReviewCategory;
import com.marlow.review.Sentiment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HeuristicReviewIntelligence implements ReviewIntelligence {

    private static final Set<String> NEGATIONS = Set.of(
            "not", "no", "never", "hardly", "without", "isn't", "wasn't", "aren't", "weren't",
            "don't", "doesn't", "didn't", "won't", "can't", "cannot", "n't"
    );

    private static final Map<String, Double> POSITIVE = Map.ofEntries(
            Map.entry("love", 1.2), Map.entry("loved", 1.2), Map.entry("amazing", 1.3),
            Map.entry("excellent", 1.3), Map.entry("great", 1.0), Map.entry("wonderful", 1.2),
            Map.entry("delicious", 1.2), Map.entry("fresh", 1.0), Map.entry("friendly", 1.0),
            Map.entry("gentle", 1.1), Map.entry("painless", 1.2), Map.entry("kind", 1.0),
            Map.entry("perfect", 1.2), Map.entry("beautiful", 1.0), Map.entry("quick", 0.8),
            Map.entry("recommend", 1.1), Map.entry("cozy", 0.8), Map.entry("tasty", 1.0),
            Map.entry("polite", 0.8), Map.entry("helpful", 0.9), Map.entry("clean", 0.7),
            Map.entry("professional", 1.0), Map.entry("outstanding", 1.3), Map.entry("happy", 0.9),
            Map.entry("best", 1.0), Map.entry("smooth", 0.8), Map.entry("prompt", 0.8),
            Map.entry("flaky", 0.9), Map.entry("warm", 0.7), Map.entry("careful", 0.9),
            Map.entry("fair", 0.6), Map.entry("lovely", 1.0), Map.entry("thank", 0.5)
    );

    private static final Map<String, Double> NEGATIVE = Map.ofEntries(
            Map.entry("bad", 1.0), Map.entry("terrible", 1.4), Map.entry("awful", 1.3),
            Map.entry("rude", 1.3), Map.entry("stale", 1.2), Map.entry("cold", 0.8),
            Map.entry("late", 1.0), Map.entry("slow", 0.9), Map.entry("dirty", 1.2),
            Map.entry("painful", 1.2), Map.entry("worst", 1.4), Map.entry("horrible", 1.3),
            Map.entry("disappointed", 1.1), Map.entry("crushed", 1.1), Map.entry("sour", 0.8),
            Map.entry("overpriced", 1.1), Map.entry("expensive", 0.8), Map.entry("ignored", 1.2),
            Map.entry("burnt", 1.2), Map.entry("mold", 1.4), Map.entry("careless", 1.1),
            Map.entry("bland", 0.9), Map.entry("dry", 0.7), Map.entry("delayed", 1.0),
            Map.entry("delay", 0.9), Map.entry("missed", 1.0), Map.entry("hurt", 1.0),
            Map.entry("hurts", 1.0), Map.entry("waited", 0.9), Map.entry("waiting", 0.8),
            Map.entry("unprofessional", 1.2), Map.entry("refund", 0.7), Map.entry("soggy", 1.0),
            Map.entry("wrong", 0.9), Map.entry("broken", 1.0), Map.entry("rushed", 0.8)
    );

    private static final Map<ReviewCategory, String[]> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(ReviewCategory.DELIVERY, new String[]{
                "delivery", "delivered", "courier", "shipping", "package", "packaging", "doorstep", "boxed", "arrived"
        });
        KEYWORDS.put(ReviewCategory.PRICING, new String[]{
                "price", "priced", "expensive", "overpriced", "bill", "charged", "charge", "cost", "cheap", "affordable", "rupee", "rupees", "value"
        });
        KEYWORDS.put(ReviewCategory.SUPPORT, new String[]{
                "called", "email", "receptionist", "manager", "support", "helpline", "replied", "response", "complaint"
        });
        KEYWORDS.put(ReviewCategory.AMBIENCE, new String[]{
                "ambience", "seating", "music", "clean", "dirty", "smell", "parking", "cozy", "atmosphere", "hygiene", "interior", "crowd"
        });
        KEYWORDS.put(ReviewCategory.SERVICE, new String[]{
                "appointment", "treatment", "gentle", "dentist", "staff", "service", "doctor", "counter", "barista", "nurse", "team"
        });
        KEYWORDS.put(ReviewCategory.PRODUCT, new String[]{
                "taste", "bread", "cake", "pastry", "croissant", "coffee", "stale", "fresh", "filling", "crown", "tooth", "teeth",
                "bun", "sourdough", "flaky", "soup", "cookie", "chai", "aligner"
        });
    }

    @Override
    public String mode() {
        return "heuristic";
    }

    @Override
    public ReviewInsight analyze(AnalysisInput input) {
        List<String> tokens = tokenize(input.text());
        double positive = 0;
        double negative = 0;
        List<String> hitWords = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            boolean negated = negated(tokens, i);
            if (POSITIVE.containsKey(token)) {
                double weight = POSITIVE.get(token);
                if (negated) {
                    negative += weight;
                } else {
                    positive += weight;
                    hitWords.add(token);
                }
            } else if (NEGATIVE.containsKey(token)) {
                double weight = NEGATIVE.get(token);
                if (negated) {
                    positive += weight * 0.6;
                } else {
                    negative += weight;
                    hitWords.add(token);
                }
            }
        }

        double textScore = (positive - negative) / Math.max(3.0, positive + negative);
        boolean textSilent = positive == 0 && negative == 0;
        double score;
        if (input.rating() == null) {
            score = textScore;
        } else {
            double ratingSignal = (Math.clamp(input.rating(), 1, 5) - 3) / 2.0;
            score = textSilent ? ratingSignal : (0.45 * ratingSignal) + (0.55 * textScore);
        }
        score = Math.clamp(score, -1.0, 1.0);

        Sentiment sentiment;
        if (score >= 0.18) {
            sentiment = Sentiment.POSITIVE;
        } else if (score <= -0.18) {
            sentiment = Sentiment.NEGATIVE;
        } else {
            sentiment = Sentiment.NEUTRAL;
        }

        ReviewCategory category = categoryFor(tokens);
        List<String> themes = themes(hitWords, category, input.text());
        String reply = draft(input, sentiment, themes);
        return new ReviewInsight(sentiment, round(score), category, themes, reply, "heuristic");
    }

    private static ReviewCategory categoryFor(List<String> tokens) {
        ReviewCategory best = ReviewCategory.OTHER;
        int bestHits = 0;
        for (Map.Entry<ReviewCategory, String[]> entry : KEYWORDS.entrySet()) {
            int hits = 0;
            for (String keyword : entry.getValue()) {
                if (tokens.contains(keyword)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = entry.getKey();
            }
        }
        return best;
    }

    private static List<String> themes(List<String> hitWords, ReviewCategory category, String text) {
        List<String> themes = new ArrayList<>();
        for (String word : hitWords) {
            if (themes.size() == 3) {
                break;
            }
            if (!themes.contains(word)) {
                themes.add(word);
            }
        }
        if (themes.isEmpty()) {
            themes.add(category == ReviewCategory.OTHER ? "general visit" : category.name().toLowerCase(Locale.ROOT));
        }
        String snippet = firstClause(text);
        if (snippet != null && themes.stream().noneMatch(snippet::contains)) {
            themes.add(snippet);
        }
        return themes.stream().limit(3).toList();
    }

    private static String draft(AnalysisInput input, Sentiment sentiment, List<String> themes) {
        String name = firstName(input.author());
        String clause = firstClause(input.text());
        String detail = clause == null || clause.isBlank()
                ? (themes.isEmpty() ? "your visit" : themes.get(0))
                : clause;
        String tone = input.tone() == null ? "warm" : input.tone().toLowerCase(Locale.ROOT);
        return switch (sentiment) {
            case POSITIVE -> positiveReply(name, detail, tone);
            case NEGATIVE -> negativeReply(name, detail, tone);
            case NEUTRAL -> neutralReply(name, detail, tone);
        };
    }

    private static String positiveReply(String name, String detail, String tone) {
        if ("brief".equals(tone)) {
            return "Hi " + name + ", thank you. Good to hear: " + detail + ". We will tell the team.";
        }
        if ("accountable".equals(tone)) {
            return "Hi " + name + ", we read this properly. \"" + capitalize(detail) + "\" is the standard we want every visit. Thank you for saying so.";
        }
        return "Hi " + name + ", this made the counter smile. \"" + capitalize(detail) + "\" We will pass it to the people who were on that shift.";
    }

    private static String negativeReply(String name, String detail, String tone) {
        if ("brief".equals(tone)) {
            return "Hi " + name + ", sorry about this: " + detail + ". Write back with the visit time and we will make it right.";
        }
        if ("accountable".equals(tone)) {
            return "Hi " + name + ", this should not have happened: " + detail + ". That sits with us. Tell us when you came in and we will fix it with you, not with a stock reply.";
        }
        return "Hi " + name + ", sorry about this: " + detail + ". Tell us the day you came and a good way to reach you, and we will sort it personally.";
    }

    private static String neutralReply(String name, String detail, String tone) {
        if ("brief".equals(tone)) {
            return "Hi " + name + ", thanks for the note: " + detail + ". If one thing would have made the visit better, we want to hear it.";
        }
        return "Hi " + name + ", thank you for taking the time. We noted this: " + detail + ". If there is one thing we should change next visit, reply here and it goes to the owner.";
    }

    private static boolean negated(List<String> tokens, int index) {
        int start = Math.max(0, index - 3);
        for (int i = start; i < index; i++) {
            String token = tokens.get(i);
            if (NEGATIONS.contains(token) || token.endsWith("n't")) {
                return true;
            }
        }
        return false;
    }

    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z']+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.endsWith("n't")) {
                tokens.add(part.substring(0, part.length() - 3));
                tokens.add("n't");
            } else {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private static String firstName(String author) {
        if (author == null || author.isBlank()) {
            return "there";
        }
        return author.trim().split("\\s+")[0];
    }

    private static String firstClause(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        String clause = cleaned.split("[.!?]")[0].trim();
        if (clause.length() > 90) {
            clause = clause.substring(0, 87).trim() + "...";
        }
        return clause.toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static double round(double score) {
        return Math.round(score * 100.0) / 100.0;
    }
}
