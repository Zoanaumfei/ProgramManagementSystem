package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
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

