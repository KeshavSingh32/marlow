package com.marlow.upload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadFileRepository extends JpaRepository<UploadFile, UUID> {
}
