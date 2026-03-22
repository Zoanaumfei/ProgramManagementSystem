package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;

public record OperationResponse(
        String id,
        String title,
        String description,
        String tenantId,
        TenantType tenantType,
        String createdBy,
        OperationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant approvedAt,
        Instant rejectedAt,
        Instant reopenedAt,
        Instant reprocessedAt) {

    public static OperationResponse from(OperationRecord record) {
        return new OperationResponse(
                record.id(),
                record.title(),
                record.description(),
                record.tenantId(),
                record.tenantType(),
                record.createdBy(),
                record.status(),
                record.createdAt(),
                record.updatedAt(),
                record.submittedAt(),
                record.approvedAt(),
                record.rejectedAt(),
                record.reopenedAt(),
                record.reprocessedAt());
    }
}

