package com.oryzem.programmanagementsystem.operations;

import java.time.Instant;

public record OperationActionResponse(
        String operationId,
        String action,
        OperationStatus status,
        Instant performedAt,
        String result) {
}
