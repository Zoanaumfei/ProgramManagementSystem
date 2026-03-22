package com.oryzem.programmanagementsystem.modules.operations;

import java.time.Instant;

public record OperationActionResponse(
        String operationId,
        String action,
        OperationStatus status,
        Instant performedAt,
        String result) {
}
