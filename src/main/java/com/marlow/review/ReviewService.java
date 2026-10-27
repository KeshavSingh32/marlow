package com.marlow.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlow.common.ApiException;
import com.marlow.intelligence.AnalysisInput;
import com.marlow.intelligence.ReviewInsight;
import com.marlow.intelligence.ReviewIntelligence;
import com.marlow.tenant.Tenant;
import com.marlow.tenant.TenantContext;
import com.marlow.tenant.TenantService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final TenantService tenants;
    private final ReviewIntelligence intelligence;
    private final ObjectMapper objectMapper;

    public ReviewService(ReviewRepository reviews, TenantService tenants, ReviewIntelligence intelligence,
                         ObjectMapper objectMapper) {
        this.reviews = reviews;
        this.tenants = tenants;
        this.intelligence = intelligence;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReviewResponse ingest(CreateReviewRequest request) {
        Tenant tenant = tenants.requireTenant(TenantContext.require());
        Review review = new Review();
        review.setTenantId(tenant.getId());
        review.setAuthorName(blankTo(request.author(), "Guest"));
        review.setRating(request.rating());
        review.setSource(blankTo(request.source(), "Desk"));
        review.setBody(request.text().trim());
        review.setReviewedAt(request.reviewedAt() == null ? Instant.now() : request.reviewedAt());
        applyInsight(review, tenant, request.tone());
        return ReviewMapper.toResponse(reviews.save(review), objectMapper);
    }

    @Transactional(readOnly = true)
    public ReviewPage list(Sentiment sentiment, ReviewCategory category, int page, int size) {
        UUID tenantId = TenantContext.require();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest request = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("reviewedAt"), Sort.Order.desc("id")));
        Page<Review> result = reviews.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (sentiment != null) {
                predicates.add(cb.equal(root.get("sentiment"), sentiment));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, request);
        List<ReviewResponse> items = result.getContent().stream()
                .map(review -> ReviewMapper.toResponse(review, objectMapper))
                .toList();
        return new ReviewPage(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public ReviewResponse redraft(UUID id, String tone) {
        Review review = require(id);
        Tenant tenant = tenants.requireTenant(review.getTenantId());
        applyInsight(review, tenant, tone);
        review.setReplyStatus(ReplyStatus.DRAFT);
        return ReviewMapper.toResponse(reviews.save(review), objectMapper);
    }

    @Transactional
    public ReviewResponse updateReply(UUID id, UpdateReplyRequest request) {
        if ((request.draftReply() == null || request.draftReply().isBlank()) && request.replyStatus() == null) {
            throw ApiException.badRequest("Send a reply or a status to update");
        }
        Review review = require(id);
        if (request.draftReply() != null && !request.draftReply().isBlank()) {
            review.setDraftReply(request.draftReply().trim());
        }
        if (request.replyStatus() != null) {
            review.setReplyStatus(request.replyStatus());
        }
        return ReviewMapper.toResponse(reviews.save(review), objectMapper);
    }

    private void applyInsight(Review review, Tenant tenant, String tone) {
        ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                tenant.getName(),
                tenant.getIndustry(),
                review.getAuthorName(),
                review.getRating(),
                review.getSource(),
                review.getBody(),
                tone
        ));
        review.setSentiment(insight.sentiment());
        review.setSentimentScore(insight.score());
        review.setCategory(insight.category());
        review.setThemes(ReviewMapper.writeThemes(insight.themes(), objectMapper));
        review.setDraftReply(insight.draftReply());
        review.setModelName(insight.model());
        if (review.getReplyStatus() == null) {
            review.setReplyStatus(ReplyStatus.DRAFT);
        }
    }

    private Review require(UUID id) {
        return reviews.findByIdAndTenantId(id, TenantContext.require())
                .orElseThrow(() -> ApiException.notFound("Review not found"));
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
