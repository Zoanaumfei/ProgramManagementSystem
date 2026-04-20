package com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;

public record DocumentReadModel(
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

    public DocumentView toView() {
        return new DocumentView(
                id,
                tenantId,
                contextType,
                contextId,
                ownerOrganizationId,
                originalFilename,
                safeFilename,
                contentType,
                extension,
                sizeBytes,
                checksumSha256,
                status,
                uploadedByUserId,
                uploadedByOrganizationId,
                createdAt,
                updatedAt,
                deletedAt);
    }
}