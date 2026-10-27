package com.marlow.upload;

import java.time.Instant;
import java.util.UUID;

public record UploadResponse(
        UUID id,
        String filename,
        String status,
        int totalRows,
        int processedRows,
        int failedRows,
        String errorMessage,
        Instant createdAt,
        Instant finishedAt
) {
    public static UploadResponse from(UploadJob job) {
        return new UploadResponse(
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus().name(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getFailedRows(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }
}
