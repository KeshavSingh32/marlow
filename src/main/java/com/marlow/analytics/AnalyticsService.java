package com.marlow.analytics;

import com.marlow.review.ReplyStatus;
import com.marlow.review.ReviewCategory;
import com.marlow.review.ReviewRepository;
import com.marlow.review.Sentiment;
import com.marlow.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final ReviewRepository reviews;
    private final Clock clock;
    private final ZoneId zone;

    public AnalyticsService(ReviewRepository reviews, Clock clock, ZoneId zone) {
        this.reviews = reviews;
        this.clock = clock;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        RangeWindow window = window(from, to);
        UUID tenantId = TenantContext.require();
        long count = 0;
        double scoreSum = 0;
        double ratingSum = 0;
        long rated = 0;
        long positive = 0;
        long neutral = 0;
        long negative = 0;
        for (Object[] row : reviews.sentimentBuckets(tenantId, window.start(), window.end())) {
            Sentiment sentiment = (Sentiment) row[0];
            long bucket = ((Number) row[1]).longValue();
            double averageScore = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
            count += bucket;
            scoreSum += averageScore * bucket;
            if (row[3] != null) {
                ratingSum += ((Number) row[3]).doubleValue() * bucket;
                rated += bucket;
            }
            if (sentiment == Sentiment.POSITIVE) {
                positive = bucket;
            } else if (sentiment == Sentiment.NEUTRAL) {
                neutral = bucket;
            } else if (sentiment == Sentiment.NEGATIVE) {
                negative = bucket;
            }
        }
        long drafts = 0;
        long sent = 0;
        for (Object[] row : reviews.replyBuckets(tenantId, window.start(), window.end())) {
            long bucket = ((Number) row[1]).longValue();
            if (row[0] == ReplyStatus.SENT) {
                sent = bucket;
            } else {
                drafts += bucket;
            }
        }
        return new SummaryResponse(
                window.from(),
                window.to(),
                count,
                count == 0 ? 0 : round(scoreSum / count),
                rated == 0 ? 0 : round(ratingSum / rated),
                share(positive, count),
                share(neutral, count),
                share(negative, count),
                positive,
                neutral,
                negative,
                drafts,
                sent
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdown> categories(LocalDate from, LocalDate to) {
        RangeWindow window = window(from, to);
        List<CategoryBreakdown> breakdowns = new ArrayList<>();
        for (Object[] row : reviews.categoryBuckets(TenantContext.require(), window.start(), window.end())) {
            long count = ((Number) row[1]).longValue();
            double score = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
            breakdowns.add(new CategoryBreakdown(((ReviewCategory) row[0]).name(), count, round(score)));
        }
        return breakdowns;
    }

    @Transactional(readOnly = true)
    public List<SourceBreakdown> sources(LocalDate from, LocalDate to) {
        RangeWindow window = window(from, to);
        List<SourceBreakdown> breakdowns = new ArrayList<>();
        for (Object[] row : reviews.sourceBuckets(TenantContext.require(), window.start(), window.end())) {
            String source = row[0] == null ? "Unknown" : row[0].toString();
            long count = ((Number) row[1]).longValue();
            double score = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
            breakdowns.add(new SourceBreakdown(source, count, round(score)));
        }
        return breakdowns;
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> trend(LocalDate from, LocalDate to) {
        RangeWindow window = window(from, to);
        List<TrendPoint> points = new ArrayList<>();
        for (Object[] row : reviews.trend(TenantContext.require(), window.start(), window.end())) {
            long count = ((Number) row[1]).longValue();
            double score = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
            double rating = row[3] == null ? 0 : ((Number) row[3]).doubleValue();
            points.add(new TrendPoint(toDate(row[0]), count, round(score), round(rating)));
        }
        return points;
    }

    public RangeWindow window(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate endDay = to == null ? today : to;
        LocalDate startDay = from == null ? endDay.minusDays(89) : from;
        if (startDay.isAfter(endDay)) {
            LocalDate swap = startDay;
            startDay = endDay;
            endDay = swap;
        }
        ZonedDateTime start = startDay.atStartOfDay(zone);
        ZonedDateTime end = endDay.plusDays(1).atStartOfDay(zone);
        return new RangeWindow(startDay, endDay, start.toInstant(), end.toInstant());
    }

    private static LocalDate toDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.of("UTC")).toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private static double share(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return round(part / (double) total);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
