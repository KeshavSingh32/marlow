package com.marlow.batchjob;

import com.marlow.review.Review;
import com.marlow.review.ReviewRepository;
import com.marlow.upload.UploadJobRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ReviewImportWriter implements ItemWriter<Review> {

    private final ReviewRepository reviews;
    private final UploadJobRepository jobs;

    public ReviewImportWriter(ReviewRepository reviews, UploadJobRepository jobs) {
        this.reviews = reviews;
        this.jobs = jobs;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends Review> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        reviews.saveAll(chunk.getItems());
        UUID jobId = chunk.getItems().get(0).getUploadJobId();
        if (jobId != null) {
            jobs.incrementProcessed(jobId, chunk.size());
        }
    }
}
