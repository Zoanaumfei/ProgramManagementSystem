package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceMapper {

    public DocumentRecord toRecord(DocumentEntity entity) {
        return new DocumentRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getOriginalFilename(),
                entity.getSafeFilename(),
                entity.getContentType(),
                entity.getExtension(),
                entity.getSizeBytes(),
                entity.getChecksumSha256(),
                entity.getStorageProvider(),
                entity.getStorageKey(),
                entity.getStatus(),
                entity.getUploadedByUserId(),
                entity.getUploadedByOrganizationId(),
                entity.getUploadExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    public DocumentEntity toNewEntity(DocumentRecord record) {
        return new DocumentEntity(
                record.id(),
                record.tenantId(),
                record.originalFilename(),
                record.safeFilename(),
                record.contentType(),
                record.extension(),
                record.sizeBytes(),
                record.checksumSha256(),
                record.storageProvider(),
                record.storageKey(),
                record.status(),
                record.uploadedByUserId(),
                record.uploadedByOrganizationId(),
                record.uploadExpiresAt(),
                record.createdAt(),
                record.updatedAt(),
                record.deletedAt());
    }

    public void apply(DocumentEntity entity, DocumentRecord record) {
        entity.updateFrom(
                record.tenantId(),
                record.originalFilename(),
                record.safeFilename(),
                record.contentType(),
                record.extension(),
                record.sizeBytes(),
                record.checksumSha256(),
                record.storageProvider(),
                record.storageKey(),
                record.status(),
                record.uploadedByUserId(),
                record.uploadedByOrganizationId(),
                record.uploadExpiresAt(),
                record.createdAt(),
                record.updatedAt(),
                record.deletedAt());
    }
}