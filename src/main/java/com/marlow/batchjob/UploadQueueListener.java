package com.marlow.batchjob;

import com.marlow.config.RabbitNames;
import com.marlow.upload.UploadFile;
import com.marlow.upload.UploadFileRepository;
import com.marlow.upload.UploadJob;
import com.marlow.upload.UploadJobRepository;
import com.marlow.upload.UploadMessage;
import com.marlow.upload.UploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class UploadQueueListener {

    private static final Logger log = LoggerFactory.getLogger(UploadQueueListener.class);

    private final JobLauncher jobLauncher;
    private final Job reviewCsvImportJob;
    private final UploadJobRepository jobs;
    private final UploadFileRepository files;

    public UploadQueueListener(JobLauncher jobLauncher, Job reviewCsvImportJob, UploadJobRepository jobs,
                               UploadFileRepository files) {
        this.jobLauncher = jobLauncher;
        this.reviewCsvImportJob = reviewCsvImportJob;
        this.jobs = jobs;
        this.files = files;
    }

    @RabbitListener(queues = RabbitNames.QUEUE)
    public void onUpload(UploadMessage message) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("jobId", message.jobId().toString())
                .addString("tenantId", message.tenantId().toString())
                .addString("filePath", message.filePath())
                .toJobParameters();
        try {
            restoreFile(message);
            jobLauncher.run(reviewCsvImportJob, parameters);
        } catch (JobInstanceAlreadyCompleteException alreadyDone) {
            log.info("Upload {} was already imported", message.jobId());
        } catch (Exception exception) {
            log.error("Upload {} could not start", message.jobId(), exception);
            jobs.findById(message.jobId()).ifPresent(job -> markFailed(job, exception.getMessage()));
        }
    }

    private void restoreFile(UploadMessage message) throws IOException {
        Path path = Path.of(message.filePath());
        if (Files.exists(path)) {
            return;
        }
        UploadFile upload = files.findById(message.jobId())
                .orElseThrow(() -> new IOException("The uploaded CSV was not found. Upload it again."));
        Files.createDirectories(path.getParent());
        Files.write(path, upload.getContent());
    }

    private void markFailed(UploadJob job, String message) {
        if (job.getStatus() == UploadStatus.COMPLETED) {
            return;
        }
        job.setStatus(UploadStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(message == null ? "Import failed" : message.substring(0, Math.min(480, message.length())));
        jobs.save(job);
    }
}
