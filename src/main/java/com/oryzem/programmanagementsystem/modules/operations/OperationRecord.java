package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;

public record OperationRecord(
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

    public OperationRecord withContent(String newTitle, String newDescription, Instant now) {
        return new OperationRecord(
                id,
                newTitle,
                newDescription,
                tenantId,
                tenantType,
                createdBy,
                status,
                createdAt,
                now,
                submittedAt,
                approvedAt,
                rejectedAt,
                reopenedAt,
                reprocessedAt);
    }

    public OperationRecord withStatus(OperationStatus newStatus, Instant now) {
        Instant newSubmittedAt = submittedAt;
        Instant newApprovedAt = approvedAt;
        Instant newRejectedAt = rejectedAt;
        Instant newReopenedAt = reopenedAt;
        Instant newReprocessedAt = reprocessedAt;

        switch (newStatus) {
            case SUBMITTED -> newSubmittedAt = now;
            case APPROVED -> newApprovedAt = now;
            case REJECTED -> newRejectedAt = now;
            case RETURNED -> newReopenedAt = now;
            case REPROCESSING -> newReprocessedAt = now;
            default -> {
            }
        }

        return new OperationRecord(
                id,
                title,
                description,
                tenantId,
                tenantType,
                createdBy,
                newStatus,
                createdAt,
                now,
                newSubmittedAt,
                newApprovedAt,
                newRejectedAt,
                newReopenedAt,
                newReprocessedAt);
    }
}

