package com.oryzem.programmanagementsystem.reports;

import com.oryzem.programmanagementsystem.operations.OperationStatus;
import java.time.Instant;

public record OperationsExportItem(
        String id,
        String title,
        OperationStatus status,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String description) {
}
