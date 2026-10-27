package com.marlow.intelligence;

import com.marlow.review.ReviewCategory;
import com.marlow.review.Sentiment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicReviewIntelligenceTest {

    private final HeuristicReviewIntelligence intelligence = new HeuristicReviewIntelligence();

    @Test
    void readsAStalePastryAsANegativeProductReview() {
        ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                "Elm & Oak Bakery", "bakery", "Kabir", 1, "Google",
                "The croissant was stale and the coffee tasted burnt. Terrible morning.", "warm"));

        assertEquals(Sentiment.NEGATIVE, insight.sentiment());
        assertEquals(ReviewCategory.PRODUCT, insight.category());
        assertTrue(insight.score() < 0);
        assertTrue(insight.draftReply().startsWith("Hi Kabir"));
        assertFalse(insight.draftReply().toLowerCase().contains("we value your feedback"));
    }

    @Test
    void readsAGentleCleaningAsAPositiveServiceReview() {
        ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                "Brightline Dental", "dental clinic", "Rahul Khanna", 5, "Practo",
                "Dr. Sethi was gentle and the cleaning was painless. I would recommend Brightline.", "brief"));

        assertEquals(Sentiment.POSITIVE, insight.sentiment());
        assertEquals(ReviewCategory.SERVICE, insight.category());
        assertTrue(insight.draftReply().contains("Rahul"));
    }

    @Test
    void flipsANegatedCompliment() {
        ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                "Elm & Oak Bakery", "bakery", "Tara", 2, "Google",
                "The bun was not fresh and the counter was not friendly.", "accountable"));

        assertEquals(Sentiment.NEGATIVE, insight.sentiment());
        assertTrue(insight.draftReply().contains("should not have happened"));
    }
}
