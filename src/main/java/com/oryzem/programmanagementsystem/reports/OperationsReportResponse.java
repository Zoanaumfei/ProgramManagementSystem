package com.oryzem.programmanagementsystem.reports;

import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.time.Instant;
import java.util.List;

public record OperationsReportResponse(
        Instant generatedAt,
        String tenantId,
        TenantType tenantType,
        String statusFilter,
        int totalOperations,
        List<OperationsReportItem> items) {
}
