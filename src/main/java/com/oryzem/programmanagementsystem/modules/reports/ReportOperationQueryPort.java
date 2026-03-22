package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.util.List;

public interface ReportOperationQueryPort {

    List<ReportOperationView> findOperations(String tenantId, String statusFilter);

    record ReportOperationView(
            String id,
            String title,
            String description,
            String tenantId,
            TenantType tenantType,
            String createdBy,
            String status,
            Instant createdAt,
            Instant updatedAt) {
    }
}
