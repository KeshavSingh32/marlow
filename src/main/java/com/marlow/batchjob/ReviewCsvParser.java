package com.marlow.batchjob;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class ReviewCsvParser {

    private static final List<DateTimeFormatter> LOCAL_DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("uuuu/M/d")
    );

    private ReviewCsvParser() {
    }

    public static Integer rating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.trim().toLowerCase(Locale.ROOT).replace("stars", "").replace("star", "").trim();
        try {
            double value = Double.parseDouble(digits);
            if (value > 5 && value <= 100) {
                value = value / 20.0;
            }
            int rounded = (int) Math.round(value);
            if (rounded < 1 || rounded > 5) {
                return null;
            }
            return rounded;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static Instant reviewedAt(String raw, ZoneId zone, Instant fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String text = raw.trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // try the shop-local formats below
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return LocalDateTime.parse(text).atZone(zone).toInstant();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        for (DateTimeFormatter formatter : LOCAL_DATES) {
            try {
                return LocalDate.parse(text, formatter).atStartOfDay(zone).toInstant();
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        return fallback;
    }
}
