package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrganizationLookup {

    Optional<OrganizationView> findById(String organizationId);

    default OrganizationView getRequired(String organizationId) {
        return findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    List<OrganizationView> findAll();

    Set<String> collectSubtreeIds(String organizationId);

    boolean isSameOrDescendant(String ancestorOrganizationId, String targetOrganizationId);

    long countDirectChildren(String organizationId);

    boolean isSetupComplete(String organizationId);

    record OrganizationView(
            String id,
            String name,
            String code,
            String tenantId,
            String marketId,
            TenantType tenantType,
            boolean active,
            String parentOrganizationId,
            String customerOrganizationId,
            Integer hierarchyLevel) {
    }
}
