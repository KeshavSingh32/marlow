package com.marlow.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_tenant_reviewed", columnList = "tenant_id,reviewed_at"),
        @Index(name = "idx_reviews_tenant_sentiment", columnList = "tenant_id,sentiment"),
        @Index(name = "idx_reviews_tenant_category", columnList = "tenant_id,category")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_tenant_external", columnNames = {"tenant_id", "external_ref"})
})
public class Review {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "author_name", nullable = false, length = 160)
    private String authorName;

    private Integer rating;

    @Column(length = 80)
    private String source;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sentiment sentiment;

    @Column(name = "sentiment_score", nullable = false)
    private double sentimentScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewCategory category;

    @Column(length = 800)
    private String themes;

    @Column(name = "draft_reply", nullable = false, columnDefinition = "text")
    private String draftReply;

    @Enumerated(EnumType.STRING)
    @Column(name = "reply_status", nullable = false, length = 20)
    private ReplyStatus replyStatus;

    @Column(name = "model_name", nullable = false, length = 80)
    private String modelName;

    @Column(name = "upload_job_id")
    private UUID uploadJobId;

    @Column(name = "external_ref", length = 120)
    private String externalRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (reviewedAt == null) {
            reviewedAt = createdAt;
        }
        if (replyStatus == null) {
            replyStatus = ReplyStatus.DRAFT;
        }
    }
}
