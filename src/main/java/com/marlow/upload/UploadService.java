package com.marlow.upload;

import com.marlow.common.ApiException;
import com.marlow.config.RabbitNames;
import com.marlow.tenant.TenantContext;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private final UploadJobRepository jobs;
    private final RabbitTemplate rabbitTemplate;
    private final Path uploadRoot;

    public UploadService(UploadJobRepository jobs, RabbitTemplate rabbitTemplate,
                         @Value("${marlow.storage.uploads}") String uploadRoot) {
        this.jobs = jobs;
        this.rabbitTemplate = rabbitTemplate;
        this.uploadRoot = Path.of(uploadRoot);
    }

    @Transactional
    public UploadResponse accept(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a CSV file first");
        }
        String original = file.getOriginalFilename() == null ? "reviews.csv" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".csv")) {
            throw ApiException.badRequest("Upload a .csv file with a header row");
        }
        UUID tenantId = TenantContext.require();
        UUID jobId = UUID.randomUUID();
        Path directory = uploadRoot.resolve(tenantId.toString());
        Path stored = directory.resolve(jobId + ".csv");
        try {
            Files.createDirectories(directory);
            file.transferTo(stored);
        } catch (IOException exception) {
            throw ApiException.unavailable("Could not store that CSV");
        }

        UploadJob job = new UploadJob();
        job.setId(jobId);
        job.setTenantId(tenantId);
        job.setOriginalFilename(sanitize(original));
        job.setStoredPath(stored.toAbsolutePath().toString());
        job.setStatus(UploadStatus.QUEUED);
        job.setCreatedAt(Instant.now());
        jobs.saveAndFlush(job);

        try {
            rabbitTemplate.convertAndSend(RabbitNames.QUEUE, new UploadMessage(jobId, tenantId, job.getStoredPath()));
        } catch (AmqpException exception) {
            job.setStatus(UploadStatus.FAILED);
            job.setErrorMessage("RabbitMQ did not accept the upload. Is the broker running?");
            jobs.save(job);
            throw ApiException.unavailable("The upload queue is not reachable. Start RabbitMQ and try again.");
        }
        return UploadResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<UploadResponse> list() {
        return jobs.findByTenantIdOrderByCreatedAtDesc(TenantContext.require()).stream()
                .map(UploadResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UploadResponse get(UUID id) {
        return jobs.findByIdAndTenantId(id, TenantContext.require())
                .map(UploadResponse::from)
                .orElseThrow(() -> ApiException.notFound("Upload not found"));
    }

    private static String sanitize(String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9._ -]", "_");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }
}
