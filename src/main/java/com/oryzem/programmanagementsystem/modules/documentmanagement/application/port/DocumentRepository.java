package com.oryzem.programmanagementsystem.modules.documentmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    Optional<DocumentRecord> findById(String id);

    List<DocumentRecord> findAllByIdIn(Collection<String> ids);

    List<DocumentRecord> findAllByStatusAndUploadExpiresAtBefore(DocumentStatus status, Instant cutoff);

    List<DocumentRecord> findAllByStatus(DocumentStatus status);

    List<String> findTrackedStorageKeys();

    DocumentRecord save(DocumentRecord document);
}