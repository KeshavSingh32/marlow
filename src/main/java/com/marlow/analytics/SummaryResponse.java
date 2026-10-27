package com.marlow.analytics;

import java.time.LocalDate;

public record SummaryResponse(
        LocalDate from,
        LocalDate to,
        long reviewCount,
        double averageSentiment,
        double averageRating,
        double positiveShare,
        double neutralShare,
        double negativeShare,
        long positive,
        long neutral,
        long negative,
        long drafts,
        long sent
) {
}
