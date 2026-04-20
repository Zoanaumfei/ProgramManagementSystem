package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class DocumentRecordLocator {

    private final DocumentRepository documentRepository;
    private final DocumentBindingRepository bindingRepository;

    public DocumentRecordLocator(
            DocumentRepository documentRepository,
            DocumentBindingRepository bindingRepository) {
        this.documentRepository = documentRepository;
        this.bindingRepository = bindingRepository;
    }

    public LocatedDocument require(String documentId) {
        DocumentRecord document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        DocumentBindingRecord binding = bindingRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document binding", documentId));
        return new LocatedDocument(document, binding);
    }

    public record LocatedDocument(
            DocumentRecord document,
            DocumentBindingRecord binding) {
    }
}