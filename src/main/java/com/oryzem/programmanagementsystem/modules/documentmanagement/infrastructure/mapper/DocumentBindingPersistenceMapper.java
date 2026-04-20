package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentBindingPersistenceMapper {

    public DocumentBindingRecord toRecord(DocumentBindingEntity entity) {
        return new DocumentBindingRecord(
                entity.getId(),
                entity.getDocumentId(),
                entity.getContextType(),
                entity.getContextId(),
                entity.getOwnerOrganizationId(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt());
    }

    public DocumentBindingEntity toNewEntity(DocumentBindingRecord record) {
        return new DocumentBindingEntity(
                record.id(),
                record.documentId(),
                record.contextType(),
                record.contextId(),
                record.ownerOrganizationId(),
                record.createdByUserId(),
                record.createdAt());
    }
}