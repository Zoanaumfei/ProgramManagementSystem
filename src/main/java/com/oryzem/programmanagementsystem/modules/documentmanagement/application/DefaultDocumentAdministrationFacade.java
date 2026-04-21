package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentAdministrationFacade implements DocumentAdministrationFacade {

    private final DocumentBindingRepository bindingRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;

    public DefaultDocumentAdministrationFacade(
            DocumentBindingRepository bindingRepository,
            DocumentRepository documentRepository,
            DocumentStorage documentStorage) {
        this.bindingRepository = bindingRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    @Override
    public DocumentPurgeSummary summarizeTrackedDocuments(List<DocumentContextRef> contexts) {
        ResolvedDocuments resolved = resolveDocuments(contexts);
        return new DocumentPurgeSummary(resolved.documentIds().size(), resolved.storageKeys().size());
    }

    @Override
    public DocumentPurgeSummary purgeTrackedDocuments(List<DocumentContextRef> contexts) {
        ResolvedDocuments resolved = resolveDocuments(contexts);
        resolved.storageKeys().forEach(documentStorage::deleteObject);
        if (!resolved.documentIds().isEmpty()) {
            bindingRepository.deleteAllByDocumentIdIn(resolved.documentIds());
            documentRepository.deleteAllByIdIn(resolved.documentIds());
        }
        return new DocumentPurgeSummary(resolved.documentIds().size(), resolved.storageKeys().size());
    }

    private ResolvedDocuments resolveDocuments(List<DocumentContextRef> contexts) {
        Set<String> documentIds = new LinkedHashSet<>();
        for (DocumentContextRef context : contexts) {
            bindingRepository.findAllByContextTypeAndContextIdOrderByCreatedAtDesc(context.contextType(), context.contextId())
                    .stream()
                    .map(DocumentBindingRecord::documentId)
                    .forEach(documentIds::add);
        }

        Map<String, DocumentRecord> documentsById = new LinkedHashMap<>();
        if (!documentIds.isEmpty()) {
            documentRepository.findAllByIdIn(documentIds).forEach(document -> documentsById.put(document.id(), document));
        }

        Set<String> storageKeys = new LinkedHashSet<>();
        for (String documentId : documentIds) {
            DocumentRecord document = documentsById.get(documentId);
            if (document != null && document.storageKey() != null && !document.storageKey().isBlank()) {
                storageKeys.add(document.storageKey());
            }
        }

        return new ResolvedDocuments(List.copyOf(documentIds), List.copyOf(storageKeys));
    }

    private record ResolvedDocuments(
            List<String> documentIds,
            List<String> storageKeys) {
    }
}
