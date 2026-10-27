package com.marlow.analytics;

import java.time.LocalDate;

public record TrendPoint(LocalDate day, long count, double averageSentiment, double averageRating) {
}
