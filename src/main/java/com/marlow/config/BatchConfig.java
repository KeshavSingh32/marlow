package com.marlow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlow.batchjob.ImportJobListener;
import com.marlow.batchjob.ReviewCsvReaderFactory;
import com.marlow.batchjob.ReviewCsvRow;
import com.marlow.batchjob.ReviewImportProcessor;
import com.marlow.batchjob.ReviewImportWriter;
import com.marlow.intelligence.ReviewIntelligence;
import com.marlow.review.Review;
import com.marlow.tenant.TenantRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZoneId;
import java.util.UUID;

@Configuration
public class BatchConfig {

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemReader<ReviewCsvRow> reviewCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath) throws Exception {
        return ReviewCsvReaderFactory.open(filePath);
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public ReviewImportProcessor reviewImportProcessor(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['jobId']}") String jobId,
            TenantRepository tenants,
            ZoneId zone,
            ReviewIntelligence intelligence,
            ObjectMapper objectMapper) {
        UUID shopId = UUID.fromString(tenantId);
        return new ReviewImportProcessor(
                shopId,
                UUID.fromString(jobId),
                tenants.findById(shopId).orElseThrow(),
                zone,
                intelligence,
                objectMapper
        );
    }

    @Bean
    public Step importReviewsStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<ReviewCsvRow> reviewCsvReader,
                                  ReviewImportProcessor reviewImportProcessor,
                                  ReviewImportWriter reviewImportWriter) {
        return new StepBuilder("importReviews", jobRepository)
                .<ReviewCsvRow, Review>chunk(25, transactionManager)
                .reader(reviewCsvReader)
                .processor(reviewImportProcessor)
                .writer(reviewImportWriter)
                .build();
    }

    @Bean
    public Job reviewCsvImportJob(JobRepository jobRepository, Step importReviewsStep, ImportJobListener listener) {
        return new JobBuilder("reviewCsvImportJob", jobRepository)
                .start(importReviewsStep)
                .listener(listener)
                .build();
    }
}
