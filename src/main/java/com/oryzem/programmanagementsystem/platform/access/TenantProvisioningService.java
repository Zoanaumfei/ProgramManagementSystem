package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantProvisioningService {

    private static final String DEFAULT_DATA_REGION = "sa-east-1";

    private final SpringDataTenantJpaRepository tenantRepository;

    public TenantProvisioningService(SpringDataTenantJpaRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public String tenantIdForRootOrganization(String organizationId) {
        return "TEN-" + organizationId;
    }

    public TenantEntity ensureTenantForRootOrganization(
            String organizationId,
            String name,
            String code,
            TenantType tenantType,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        String tenantId = tenantIdForRootOrganization(organizationId);
        return tenantRepository.findById(tenantId)
                .map(existing -> {
                    existing.updateFromRootOrganization(
                            name,
                            code,
                            active ? TenantStatus.ACTIVE : TenantStatus.INACTIVE,
                            tenantType,
                            DEFAULT_DATA_REGION,
                            organizationId,
                            defaultServiceTier(tenantType),
                            updatedAt != null ? updatedAt : Instant.now());
                    return tenantRepository.save(existing);
                })
                .orElseGet(() -> tenantRepository.save(TenantEntity.create(
                        tenantId,
                        name,
                        code,
                        active ? TenantStatus.ACTIVE : TenantStatus.INACTIVE,
                        tenantType,
                        DEFAULT_DATA_REGION,
                        organizationId,
                        defaultServiceTier(tenantType),
                        createdAt != null ? createdAt : Instant.now(),
                        updatedAt != null ? updatedAt : createdAt != null ? createdAt : Instant.now())));
    }

    private TenantServiceTier defaultServiceTier(TenantType tenantType) {
        return tenantType == TenantType.INTERNAL ? TenantServiceTier.INTERNAL : TenantServiceTier.STANDARD;
    }
}
