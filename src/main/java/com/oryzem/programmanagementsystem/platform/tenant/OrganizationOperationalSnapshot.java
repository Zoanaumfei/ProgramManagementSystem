package com.oryzem.programmanagementsystem.platform.tenant;

public record OrganizationOperationalSnapshot(
        String tenantId,
        long organizationCount,
        long activeOrganizationCount,
        long inactiveOrganizationCount,
        long offboardingOrganizationCount,
        long offboardedOrganizationCount,
        long purgedOrganizationCount,
        long notRequestedExportCount,
        long readyForExportCount,
        long exportInProgressCount,
        long exportedCount) {
}
