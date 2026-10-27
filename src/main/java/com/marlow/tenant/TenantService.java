package com.marlow.tenant;

import com.marlow.common.ApiException;
import com.marlow.common.ApiKeys;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenants;
    private final ShopUserRepository users;
    private final ApiKeyRepository apiKeys;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenants, ShopUserRepository users, ApiKeyRepository apiKeys,
                         PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.apiKeys = apiKeys;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Tenant createShop(String name, String slug, String industry, String city) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(slug);
        tenant.setIndustry(industry);
        tenant.setCity(city);
        return tenants.save(tenant);
    }

    @Transactional
    public ShopUser addUser(UUID tenantId, String email, String fullName, ShopRole role, String rawPassword) {
        requireTenant(tenantId);
        ShopUser user = new ShopUser();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return users.save(user);
    }

    @Transactional
    public void issueKey(UUID tenantId, String label, String rawKey) {
        requireTenant(tenantId);
        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setLabel(label);
        apiKey.setKeyPrefix(rawKey.substring(0, Math.min(16, rawKey.length())));
        apiKey.setKeyHash(ApiKeys.sha256(rawKey));
        apiKeys.save(apiKey);
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(UUID tenantId) {
        return tenants.findById(tenantId).orElseThrow(() -> ApiException.notFound("Shop not found"));
    }

    @Transactional(readOnly = true)
    public Tenant resolveKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw ApiException.unauthorized("Missing or unknown API key");
        }
        ApiKey apiKey = apiKeys.findByKeyHashAndRevokedAtIsNull(ApiKeys.sha256(rawKey.trim()))
                .orElseThrow(() -> ApiException.unauthorized("Missing or unknown API key"));
        return requireTenant(apiKey.getTenantId());
    }
}
