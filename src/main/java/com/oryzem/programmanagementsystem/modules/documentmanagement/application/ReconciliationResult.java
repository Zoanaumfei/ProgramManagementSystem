package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

public record ReconciliationResult(
        int expiredPendingMarkedFailed,
        int failedRecovered,
        int missingActiveObjects,
        int orphanObjectsDetected) {
}
