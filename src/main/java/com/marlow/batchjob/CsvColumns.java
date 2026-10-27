package com.marlow.batchjob;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class CsvColumns {

    private CsvColumns() {
    }

    public static String[] namesFor(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String header = reader.readLine();
            if (header == null || header.isBlank()) {
                throw new IOException("CSV is missing a header row");
            }
            return namesFromHeader(header);
        }
    }

    public static String[] namesFromHeader(String header) {
        String[] raw = header.split(",", -1);
        String[] names = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            names[i] = canonical(raw[i], i);
        }
        return names;
    }

    static String canonical(String header, int index) {
        String cleaned = header == null ? "" : header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        cleaned = cleaned.replace(' ', '_').replaceAll("[^a-z0-9_]", "");
        return switch (cleaned) {
            case "author", "reviewer", "name", "customer", "user", "customer_name" -> "author";
            case "rating", "stars", "score", "star_rating" -> "rating";
            case "source", "channel", "platform" -> "source";
            case "reviewed_at", "date", "created_at", "review_date", "time" -> "reviewedAt";
            case "text", "review", "comment", "body", "feedback", "content" -> "text";
            default -> "ignored" + index;
        };
    }
}
