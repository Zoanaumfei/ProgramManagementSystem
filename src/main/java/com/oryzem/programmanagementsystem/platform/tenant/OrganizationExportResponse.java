package com.oryzem.programmanagementsystem.platform.tenant;

import java.time.Instant;

record OrganizationExportResponse(
        String organizationId,
        OrganizationLifecycleState lifecycleState,
        OrganizationDataExportStatus dataExportStatus,
        boolean eligible,
        Instant offboardedAt,
        Instant retentionUntil,
        Instant dataExportedAt,
        Instant updatedAt) {
}
