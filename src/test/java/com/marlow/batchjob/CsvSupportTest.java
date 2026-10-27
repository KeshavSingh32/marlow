package com.marlow.batchjob;

import com.marlow.intelligence.SpringAiReviewIntelligence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsvSupportTest {

    @Test
    void mapsCommonHeadersOntoTheDeskColumns() {
        assertArrayEquals(
                new String[]{"author", "rating", "source", "reviewedAt", "text", "ignored5"},
                CsvColumns.namesFromHeader("Reviewer, Stars, Channel, Review Date, Comment, Notes"));
    }

    @Test
    void parsesRatingsAndShopLocalDates() {
        assertEquals(5, ReviewCsvParser.rating("5 stars"));
        assertEquals(4, ReviewCsvParser.rating("80"));
        assertNull(ReviewCsvParser.rating("lovely"));

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant parsed = ReviewCsvParser.reviewedAt("2026-10-02", zone, Instant.EPOCH);
        assertEquals(ZonedDateTime.of(2026, 10, 2, 0, 0, 0, 0, zone).toInstant(), parsed);
    }

    @Test
    void stripsMarkdownFencesFromModelJson() {
        String raw = """
                ```json
                {"sentiment":"POSITIVE","score":0.8}
                ```
                """;
        assertEquals("{\"sentiment\":\"POSITIVE\",\"score\":0.8}", SpringAiReviewIntelligence.jsonOnly(raw).replaceAll("\\s+", ""));
    }
}
