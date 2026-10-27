package com.marlow.batchjob;

import com.marlow.intelligence.AnalysisInput;
import com.marlow.intelligence.ReviewInsight;
import com.marlow.intelligence.ReviewIntelligence;
import com.marlow.review.ReplyStatus;
import com.marlow.review.Review;
import com.marlow.review.ReviewMapper;
import com.marlow.tenant.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public class ReviewImportProcessor implements ItemProcessor<ReviewCsvRow, Review> {

    private final UUID tenantId;
    private final UUID jobId;
    private final Tenant tenant;
    private final ZoneId zone;
    private final ReviewIntelligence intelligence;
    private final ObjectMapper objectMapper;
    private int line;

    public ReviewImportProcessor(UUID tenantId, UUID jobId, Tenant tenant, ZoneId zone,
                                 ReviewIntelligence intelligence, ObjectMapper objectMapper) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.tenant = tenant;
        this.zone = zone;
        this.intelligence = intelligence;
        this.objectMapper = objectMapper;
    }

    @Override
    public Review process(ReviewCsvRow row) {
        line++;
        if (row.getText() == null || row.getText().isBlank()) {
            return null;
        }
        Review review = new Review();
        review.setTenantId(tenantId);
        review.setUploadJobId(jobId);
        review.setExternalRef(jobId + ":" + line);
        review.setAuthorName(row.getAuthor() == null ? "Guest" : row.getAuthor());
        review.setRating(ReviewCsvParser.rating(row.getRating()));
        review.setSource(row.getSource() == null ? "CSV" : row.getSource());
        review.setBody(row.getText().trim());
        review.setReviewedAt(ReviewCsvParser.reviewedAt(row.getReviewedAt(), zone, Instant.now()));
        review.setReplyStatus(ReplyStatus.DRAFT);
        ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                tenant.getName(),
                tenant.getIndustry(),
                review.getAuthorName(),
                review.getRating(),
                review.getSource(),
                review.getBody(),
                "warm"
        ));
        review.setSentiment(insight.sentiment());
        review.setSentimentScore(insight.score());
        review.setCategory(insight.category());
        review.setThemes(ReviewMapper.writeThemes(insight.themes(), objectMapper));
        review.setDraftReply(insight.draftReply());
        review.setModelName(insight.model());
        return review;
    }
}
