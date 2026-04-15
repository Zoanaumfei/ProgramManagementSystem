package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "safe_filename", length = 255, nullable = false)
    private String safeFilename;

    @Column(name = "content_type", length = 160, nullable = false)
    private String contentType;

    @Column(length = 16, nullable = false)
    private String extension;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64, nullable = false)
    private String checksumSha256;

    @Column(name = "storage_provider", length = 32, nullable = false)
    private String storageProvider;

    @Column(name = "storage_key", length = 512, nullable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private DocumentStatus status;

    @Column(name = "uploaded_by_user_id", length = 64, nullable = false)
    private String uploadedByUserId;

    @Column(name = "uploaded_by_organization_id", length = 64, nullable = false)
    private String uploadedByOrganizationId;

    @Column(name = "upload_expires_at")
    private Instant uploadExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected DocumentEntity() {
    }

    public static DocumentEntity initiate(
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
        DocumentEntity entity = new DocumentEntity();
        entity.id = id;
        entity.tenantId = tenantId;
        entity.originalFilename = originalFilename;
        entity.safeFilename = safeFilename;
        entity.contentType = contentType;
        entity.extension = extension;
        entity.sizeBytes = sizeBytes;
        entity.checksumSha256 = checksumSha256;
        entity.storageProvider = storageProvider;
        entity.storageKey = storageKey;
        entity.status = DocumentStatus.PENDING_UPLOAD;
        entity.uploadedByUserId = uploadedByUserId;
        entity.uploadedByOrganizationId = uploadedByOrganizationId;
        entity.uploadExpiresAt = uploadExpiresAt;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
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

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getSafeFilename() {
        return safeFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public String getUploadedByOrganizationId() {
        return uploadedByOrganizationId;
    }

    public Instant getUploadExpiresAt() {
        return uploadExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
