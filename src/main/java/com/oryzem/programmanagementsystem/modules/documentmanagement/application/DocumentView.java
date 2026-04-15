package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;

public record DocumentView(
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
}
