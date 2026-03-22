package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.modules.operations.OperationStatus;
import java.time.Instant;

public record OperationsReportItem(
        String id,
        String title,
        String tenantId,
        TenantType tenantType,
        OperationStatus status,
        Instant createdAt,
        Instant updatedAt) {
}

