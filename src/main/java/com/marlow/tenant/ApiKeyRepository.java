package com.marlow.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<ApiKey> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);
}
