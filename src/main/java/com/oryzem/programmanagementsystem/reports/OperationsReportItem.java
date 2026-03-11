package com.oryzem.programmanagementsystem.reports;

import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.operations.OperationStatus;
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
