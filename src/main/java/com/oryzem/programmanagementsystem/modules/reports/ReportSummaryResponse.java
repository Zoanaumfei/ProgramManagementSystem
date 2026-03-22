package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.util.Map;

public record ReportSummaryResponse(
        Instant generatedAt,
        String tenantId,
        TenantType tenantType,
        long totalUsers,
        long totalOperations,
        Map<String, Long> usersByRole,
        Map<String, Long> usersByStatus,
        Map<String, Long> operationsByStatus) {

    public static ReportSummaryResponse of(
            Instant generatedAt,
            String tenantId,
            TenantType tenantType,
            long totalUsers,
            long totalOperations,
            Map<String, Long> usersByRole,
            Map<String, Long> usersByStatus,
            Map<String, Long> operationsByStatus) {
        return new ReportSummaryResponse(
                generatedAt,
                tenantId,
                tenantType,
                totalUsers,
                totalOperations,
                usersByRole,
                usersByStatus,
                operationsByStatus);
    }
}

