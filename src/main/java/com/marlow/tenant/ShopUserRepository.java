package com.marlow.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShopUserRepository extends JpaRepository<ShopUser, UUID> {

    List<ShopUser> findByTenantIdOrderByRoleAscFullNameAsc(UUID tenantId);
}
