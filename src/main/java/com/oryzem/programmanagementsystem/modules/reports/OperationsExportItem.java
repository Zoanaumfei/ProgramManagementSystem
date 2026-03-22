package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.modules.operations.OperationStatus;
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

