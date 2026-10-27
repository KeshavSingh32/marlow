package com.marlow.analytics;

import java.time.Instant;
import java.time.LocalDate;

public record RangeWindow(LocalDate from, LocalDate to, Instant start, Instant end) {
}
