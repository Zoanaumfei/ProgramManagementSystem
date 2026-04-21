package com.oryzem.programmanagementsystem.modules.documentmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface DocumentBindingRepository {

    Optional<DocumentBindingRecord> findByDocumentId(String documentId);

    List<DocumentBindingRecord> findAllByContextTypeAndContextIdOrderByCreatedAtDesc(
            DocumentContextType contextType,
            String contextId);

    long countTrackedByContext(DocumentContextType contextType, String contextId);

    List<DocumentBindingRecord> findAll();

    DocumentBindingRecord save(DocumentBindingRecord binding);

    void deleteAllByDocumentIdIn(Collection<String> documentIds);
}
