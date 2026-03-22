package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.util.List;

public record OperationsExportResponse(
        Instant generatedAt,
        String tenantId,
        TenantType tenantType,
        String statusFilter,
        boolean sensitiveDataIncluded,
        boolean masked,
        ReportSummaryResponse summary,
        List<OperationsExportItem> items) {
}

