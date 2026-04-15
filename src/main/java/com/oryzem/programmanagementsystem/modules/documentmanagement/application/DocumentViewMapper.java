package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentViewMapper {

    public DocumentView toView(DocumentEntity document, DocumentBindingEntity binding) {
        if (document == null || binding == null) {
            return null;
        }
        return new DocumentView(
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
