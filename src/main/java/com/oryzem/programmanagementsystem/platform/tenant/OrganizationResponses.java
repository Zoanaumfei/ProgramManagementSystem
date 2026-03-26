package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;

record OrganizationResponse(
        String id,
        String name,
        String code,
        String tenantId,
        String marketId,
        TenantType tenantType,
        String parentOrganizationId,
        String customerOrganizationId,
        Integer hierarchyLevel,
        int childrenCount,
        boolean hasChildren,
        OrganizationStatus status,
        OrganizationSetupStatus setupStatus,
        OrganizationUserSummaryResponse userSummary,
        boolean canInactivate,
        String inactivationBlockedReason,
        Instant createdAt,
        Instant updatedAt) {

    static OrganizationResponse from(
            OrganizationEntity organization,
            int childrenCount,
            OrganizationSetupStatus setupStatus,
            OrganizationUserSummaryResponse userSummary,
            boolean canInactivate,
            String inactivationBlockedReason) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getTenantId(),
                organization.getMarketId(),
                organization.getTenantType(),
                organization.getParentOrganization() != null ? organization.getParentOrganization().getId() : null,
                organization.getCustomerOrganization() != null ? organization.getCustomerOrganization().getId() : null,
                organization.getHierarchyLevel(),
                childrenCount,
                childrenCount > 0,
                organization.getStatus(),
                setupStatus,
                userSummary,
                canInactivate,
                inactivationBlockedReason,
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}

record OrganizationPurgeResponse(
        String organizationId,
        String action,
        Instant performedAt,
        String status,
        int purgedOrganizations,
        int purgedUsers) {
}

record OrganizationUserSummaryResponse(
        int invitedCount,
        int activeCount,
        int inactiveCount,
        int totalCount) {

    static OrganizationUserSummaryResponse empty() {
        return new OrganizationUserSummaryResponse(0, 0, 0, 0);
    }
}
