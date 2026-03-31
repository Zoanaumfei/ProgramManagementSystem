package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;

record OrganizationResponse(
        String id,
        String name,
        String cnpj,
        String tenantId,
        String marketId,
        TenantType tenantType,
        int childrenCount,
        boolean hasChildren,
        OrganizationStatus status,
        OrganizationSetupStatus setupStatus,
        OrganizationUserSummaryResponse userSummary,
        boolean canInactivate,
        String inactivationBlockedReason,
        Instant createdAt,
        Instant updatedAt,
        boolean reused) {

    static OrganizationResponse from(
            OrganizationEntity organization,
            int childrenCount,
            OrganizationSetupStatus setupStatus,
            OrganizationUserSummaryResponse userSummary,
            boolean canInactivate,
            String inactivationBlockedReason) {
        return from(
                organization,
                childrenCount,
                setupStatus,
                userSummary,
                canInactivate,
                inactivationBlockedReason,
                false);
    }

    static OrganizationResponse from(
            OrganizationEntity organization,
            int childrenCount,
            OrganizationSetupStatus setupStatus,
            OrganizationUserSummaryResponse userSummary,
            boolean canInactivate,
            String inactivationBlockedReason,
            boolean reused) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCnpj(),
                organization.getTenantId(),
                organization.getMarketId(),
                organization.getTenantType(),
                childrenCount,
                childrenCount > 0,
                organization.getStatus(),
                setupStatus,
                userSummary,
                canInactivate,
                inactivationBlockedReason,
                organization.getCreatedAt(),
                organization.getUpdatedAt(),
                reused);
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
