package com.oryzem.programmanagementsystem.modules.documentmanagement.api;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentDownloadLink;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentUploadSession;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Map;

record InitiateDocumentUploadRequest(
        @NotBlank String originalFilename,
        @NotBlank String contentType,
        @Positive long sizeBytes,
        @NotBlank String checksumSha256) {
}

record UploadSessionResponse(
        String documentId,
        String url,
        Map<String, String> fields,
        Instant expiresAt) {

    static UploadSessionResponse from(DocumentUploadSession session) {
        return new UploadSessionResponse(session.documentId(), session.url(), session.fields(), session.expiresAt());
    }
}

record DocumentResponse(
        String id,
        String tenantId,
        DocumentContextType contextType,
        String contextId,
        String ownerOrganizationId,
        String originalFilename,
        String safeFilename,
        String contentType,
        String extension,
        long sizeBytes,
        String checksumSha256,
        DocumentStatus status,
        String uploadedByUserId,
        String uploadedByOrganizationId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    static DocumentResponse from(DocumentView view) {
        return new DocumentResponse(
                view.id(),
                view.tenantId(),
                view.contextType(),
                view.contextId(),
                view.ownerOrganizationId(),
                view.originalFilename(),
                view.safeFilename(),
                view.contentType(),
                view.extension(),
                view.sizeBytes(),
                view.checksumSha256(),
                view.status(),
                view.uploadedByUserId(),
                view.uploadedByOrganizationId(),
                view.createdAt(),
                view.updatedAt(),
                view.deletedAt());
    }
}

record DownloadUrlResponse(
        String url,
        Instant expiresAt) {

    static DownloadUrlResponse from(DocumentDownloadLink link) {
        return new DownloadUrlResponse(link.url(), link.expiresAt());
    }
}
