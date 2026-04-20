package com.oryzem.programmanagementsystem.modules.documentmanagement.application.model;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;

public class DocumentRecord {

    private final String id;
    private final String tenantId;
    private final String originalFilename;
    private final String safeFilename;
    private final String contentType;
    private final String extension;
    private final long sizeBytes;
    private final String checksumSha256;
    private final String storageProvider;
    private final String storageKey;
    private DocumentStatus status;
    private final String uploadedByUserId;
    private final String uploadedByOrganizationId;
    private final Instant uploadExpiresAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public DocumentRecord(
            String id,
            String tenantId,
            String originalFilename,
            String safeFilename,
            String contentType,
            String extension,
            long sizeBytes,
            String checksumSha256,
            String storageProvider,
            String storageKey,
            DocumentStatus status,
            String uploadedByUserId,
            String uploadedByOrganizationId,
            Instant uploadExpiresAt,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.originalFilename = originalFilename;
        this.safeFilename = safeFilename;
        this.contentType = contentType;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.status = status;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedByOrganizationId = uploadedByOrganizationId;
        this.uploadExpiresAt = uploadExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static DocumentRecord initiate(
            String id,
            String tenantId,
            String originalFilename,
            String safeFilename,
            String contentType,
            String extension,
            long sizeBytes,
            String checksumSha256,
            String storageProvider,
            String storageKey,
            String uploadedByUserId,
            String uploadedByOrganizationId,
            Instant uploadExpiresAt,
            Instant now) {
        return new DocumentRecord(
                id,
                tenantId,
                originalFilename,
                safeFilename,
                contentType,
                extension,
                sizeBytes,
                checksumSha256,
                storageProvider,
                storageKey,
                DocumentStatus.PENDING_UPLOAD,
                uploadedByUserId,
                uploadedByOrganizationId,
                uploadExpiresAt,
                now,
                now,
                null);
    }

    public void markActive(Instant now) {
        if (status == DocumentStatus.DELETED) {
            throw new IllegalStateException("Deleted documents cannot become active again.");
        }
        this.status = DocumentStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void markFailed(Instant now) {
        if (status == DocumentStatus.DELETED) {
            return;
        }
        this.status = DocumentStatus.FAILED;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        if (status == DocumentStatus.DELETED) {
            return;
        }
        this.status = DocumentStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public String id() { return id; }
    public String tenantId() { return tenantId; }
    public String originalFilename() { return originalFilename; }
    public String safeFilename() { return safeFilename; }
    public String contentType() { return contentType; }
    public String extension() { return extension; }
    public long sizeBytes() { return sizeBytes; }
    public String checksumSha256() { return checksumSha256; }
    public String storageProvider() { return storageProvider; }
    public String storageKey() { return storageKey; }
    public DocumentStatus status() { return status; }
    public String uploadedByUserId() { return uploadedByUserId; }
    public String uploadedByOrganizationId() { return uploadedByOrganizationId; }
    public Instant uploadExpiresAt() { return uploadExpiresAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant deletedAt() { return deletedAt; }
}