package com.marlow.batchjob;

import com.marlow.upload.UploadJob;
import com.marlow.upload.UploadJobRepository;
import com.marlow.upload.UploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class ImportJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ImportJobListener.class);

    private final UploadJobRepository jobs;

    public ImportJobListener(UploadJobRepository jobs) {
        this.jobs = jobs;
    }

    @Override
    @Transactional
    public void beforeJob(JobExecution jobExecution) {
        update(jobExecution, true);
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        update(jobExecution, false);
    }

    private void update(JobExecution jobExecution, boolean starting) {
        String rawId = jobExecution.getJobParameters().getString("jobId");
        if (rawId == null) {
            return;
        }
        UUID jobId = UUID.fromString(rawId);
        UploadJob job = jobs.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (starting) {
            job.setStatus(UploadStatus.RUNNING);
            jobs.save(job);
            return;
        }
        long read = 0;
        long written = 0;
        long filtered = 0;
        for (StepExecution step : jobExecution.getStepExecutions()) {
            read += step.getReadCount();
            written += step.getWriteCount();
            filtered += step.getFilterCount();
        }
        job.setTotalRows((int) read);
        job.setProcessedRows((int) written);
        job.setFailedRows((int) Math.max(0, read - written - filtered));
        job.setFinishedAt(Instant.now());
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            job.setStatus(UploadStatus.COMPLETED);
            job.setErrorMessage(null);
            log.info("Imported {} reviews for upload {}", written, jobId);
        } else {
            job.setStatus(UploadStatus.FAILED);
            String message = jobExecution.getAllFailureExceptions().isEmpty()
                    ? "Import failed"
                    : jobExecution.getAllFailureExceptions().get(0).getMessage();
            job.setErrorMessage(trim(message));
            log.warn("Upload {} failed: {}", jobId, message);
        }
        jobs.save(job);
    }

    private static String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
