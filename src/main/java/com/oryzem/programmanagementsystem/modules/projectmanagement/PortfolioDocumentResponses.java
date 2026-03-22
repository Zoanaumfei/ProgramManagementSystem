package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

record DeliverableDocumentResponse(
        String id,
        String deliverableId,
        String fileName,
        String contentType,
        long fileSize,
        DeliverableDocumentStatus status,
        OffsetDateTime uploadedAt,
        Instant createdAt,
        Instant updatedAt) {

    static DeliverableDocumentResponse from(DeliverableDocumentEntity document) {
        return new DeliverableDocumentResponse(
                document.getId(),
                document.getDeliverable().getId(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getUploadedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}

record DeliverableDocumentUploadResponse(
        DeliverableDocumentResponse document,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {
}

record DeliverableDocumentDownloadResponse(
        String documentId,
        String downloadUrl,
        Instant expiresAt) {
}


