package com.marlow.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select r.sentiment, count(r), avg(r.sentimentScore), avg(r.rating)
            from Review r
            where r.tenantId = :tenantId
              and r.reviewedAt >= :from
              and r.reviewedAt < :to
            group by r.sentiment
            """)
    List<Object[]> sentimentBuckets(@Param("tenantId") UUID tenantId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    @Query("""
            select r.category, count(r), avg(r.sentimentScore)
            from Review r
            where r.tenantId = :tenantId
              and r.reviewedAt >= :from
              and r.reviewedAt < :to
            group by r.category
            order by count(r) desc
            """)
    List<Object[]> categoryBuckets(@Param("tenantId") UUID tenantId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("""
            select r.replyStatus, count(r)
            from Review r
            where r.tenantId = :tenantId
              and r.reviewedAt >= :from
              and r.reviewedAt < :to
            group by r.replyStatus
            """)
    List<Object[]> replyBuckets(@Param("tenantId") UUID tenantId,
                                @Param("from") Instant from,
                                @Param("to") Instant to);

    @Query("""
            select r.source, count(r), avg(r.sentimentScore)
            from Review r
            where r.tenantId = :tenantId
              and r.reviewedAt >= :from
              and r.reviewedAt < :to
            group by r.source
            order by count(r) desc
            """)
    List<Object[]> sourceBuckets(@Param("tenantId") UUID tenantId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Query(value = """
            select cast(reviewed_at as date) as day,
                   count(*) as review_count,
                   avg(sentiment_score) as average_score,
                   avg(rating) as average_rating
            from reviews
            where tenant_id = :tenantId
              and reviewed_at >= :from
              and reviewed_at < :to
            group by cast(reviewed_at as date)
            order by day
            """, nativeQuery = true)
    List<Object[]> trend(@Param("tenantId") UUID tenantId,
                         @Param("from") Instant from,
                         @Param("to") Instant to);
}
