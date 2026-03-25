package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.Optional;

public interface OrganizationBoundaryResolver {

    Optional<OrganizationBoundaryView> findBoundary(String organizationId);

    record OrganizationBoundaryView(
            String organizationId,
            String tenantId,
            String marketId,
            TenantType tenantType) {
    }
}
