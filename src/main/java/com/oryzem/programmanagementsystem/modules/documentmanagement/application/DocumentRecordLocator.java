package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentBindingEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentBindingJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class DocumentRecordLocator {

    private final SpringDataDocumentJpaRepository documentRepository;
    private final SpringDataDocumentBindingJpaRepository bindingRepository;

    public DocumentRecordLocator(
            SpringDataDocumentJpaRepository documentRepository,
            SpringDataDocumentBindingJpaRepository bindingRepository) {
        this.documentRepository = documentRepository;
        this.bindingRepository = bindingRepository;
    }

    public LocatedDocument require(String documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        DocumentBindingEntity binding = bindingRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document binding", documentId));
        return new LocatedDocument(document, binding);
    }

    public record LocatedDocument(
            DocumentEntity document,
            DocumentBindingEntity binding) {
    }
}
