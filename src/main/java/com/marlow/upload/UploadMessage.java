package com.marlow.upload;

import java.util.UUID;

public record UploadMessage(UUID jobId, UUID tenantId, String filePath) {
}
