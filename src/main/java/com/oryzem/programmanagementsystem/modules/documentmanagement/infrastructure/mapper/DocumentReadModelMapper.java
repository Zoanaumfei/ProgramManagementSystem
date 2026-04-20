package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.read.DocumentReadModel;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentReadModelMapper {

    public DocumentReadModel toReadModel(DocumentEntity document, DocumentBindingEntity binding) {
        if (document == null || binding == null) {
            return null;
        }
        return new DocumentReadModel(
                document.getId(),
                document.getTenantId(),
                binding.getContextType(),
                binding.getContextId(),
                binding.getOwnerOrganizationId(),
                document.getOriginalFilename(),
                document.getSafeFilename(),
                document.getContentType(),
                document.getExtension(),
                document.getSizeBytes(),
                document.getChecksumSha256(),
                document.getStatus(),
                document.getUploadedByUserId(),
                document.getUploadedByOrganizationId(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getDeletedAt());
    }
}