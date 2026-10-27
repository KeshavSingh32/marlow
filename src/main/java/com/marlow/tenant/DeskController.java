package com.marlow.tenant;

import com.marlow.intelligence.ReviewIntelligence;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DeskController {

    private final TenantService tenants;
    private final ShopUserRepository users;
    private final ApiKeyRepository apiKeys;
    private final ReviewIntelligence intelligence;

    public DeskController(TenantService tenants, ShopUserRepository users, ApiKeyRepository apiKeys,
                          ReviewIntelligence intelligence) {
        this.tenants = tenants;
        this.users = users;
        this.apiKeys = apiKeys;
        this.intelligence = intelligence;
    }

    @GetMapping("/meta")
    public MetaResponse meta() {
        return new MetaResponse("Marlow", intelligence.mode());
    }

    @GetMapping("/tenant")
    public TenantResponse tenant(HttpServletRequest request) {
        Tenant shop = (Tenant) request.getAttribute("marlow.tenant");
        if (shop == null) {
            shop = tenants.requireTenant(TenantContext.require());
        }
        List<MemberResponse> members = users.findByTenantIdOrderByRoleAscFullNameAsc(shop.getId()).stream()
                .map(user -> new MemberResponse(user.getFullName(), user.getEmail(), user.getRole().name()))
                .toList();
        List<KeyResponse> keys = apiKeys.findByTenantIdOrderByCreatedAtAsc(shop.getId()).stream()
                .map(key -> new KeyResponse(key.getLabel(), key.getKeyPrefix(), key.getRevokedAt() != null))
                .toList();
        return new TenantResponse(shop.getId(), shop.getName(), shop.getSlug(), shop.getIndustry(), shop.getCity(),
                intelligence.mode(), members, keys);
    }

    public record MetaResponse(String name, String aiMode) {
    }

    public record TenantResponse(
            UUID id,
            String name,
            String slug,
            String industry,
            String city,
            String aiMode,
            List<MemberResponse> members,
            List<KeyResponse> keys
    ) {
    }

    public record MemberResponse(String name, String email, String role) {
    }

    public record KeyResponse(String label, String prefix, boolean revoked) {
    }
}
