package com.marlow.upload;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadJobRepository extends JpaRepository<UploadJob, UUID> {

    Optional<UploadJob> findByIdAndTenantId(UUID id, UUID tenantId);

    List<UploadJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UploadJob j set j.processedRows = j.processedRows + :count where j.id = :id")
    int incrementProcessed(@Param("id") UUID id, @Param("count") int count);
}
