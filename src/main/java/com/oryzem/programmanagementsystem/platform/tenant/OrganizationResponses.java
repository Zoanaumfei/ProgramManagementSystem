package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

record OrganizationResponse(
        String id,
        String name,
        String code,
        TenantType tenantType,
        String parentOrganizationId,
        String customerOrganizationId,
        Integer hierarchyLevel,
        int childrenCount,
        boolean hasChildren,
        OrganizationStatus status,
        OrganizationSetupStatus setupStatus,
        OrganizationUserSummaryResponse userSummary,
        OrganizationProgramSummaryResponse programSummary,
        boolean canInactivate,
        String inactivationBlockedReason,
        Instant createdAt,
        Instant updatedAt) {

    static OrganizationResponse from(
            OrganizationEntity organization,
            int childrenCount,
            OrganizationSetupStatus setupStatus,
            OrganizationUserSummaryResponse userSummary,
            OrganizationProgramSummaryResponse programSummary,
            boolean canInactivate,
            String inactivationBlockedReason) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getTenantType(),
                organization.getParentOrganization() != null ? organization.getParentOrganization().getId() : null,
                organization.getCustomerOrganization() != null ? organization.getCustomerOrganization().getId() : null,
                organization.getHierarchyLevel(),
                childrenCount,
                childrenCount > 0,
                organization.getStatus(),
                setupStatus,
                userSummary,
                programSummary,
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
        int purgedPrograms,
        int purgedUsers,
        int purgedDocuments) {
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

record OrganizationProgramSummaryResponse(
        int ownedCount,
        int participatingCount,
        int totalCount) {

    static OrganizationProgramSummaryResponse empty() {
        return new OrganizationProgramSummaryResponse(0, 0, 0);
    }
}


