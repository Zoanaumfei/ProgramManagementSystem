package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper.DocumentPersistenceMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDocumentRepositoryAdapter implements DocumentRepository {

    private final SpringDataDocumentJpaRepository delegate;
    private final DocumentPersistenceMapper mapper;

    public JpaDocumentRepositoryAdapter(
            SpringDataDocumentJpaRepository delegate,
            DocumentPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<DocumentRecord> findById(String id) {
        return delegate.findById(id).map(mapper::toRecord);
    }

    @Override
    public List<DocumentRecord> findAllByIdIn(Collection<String> ids) {
        return delegate.findAllByIdIn(ids).stream().map(mapper::toRecord).toList();
    }

    @Override
    public List<DocumentRecord> findAllByStatusAndUploadExpiresAtBefore(DocumentStatus status, Instant cutoff) {
        return delegate.findAllByStatusAndUploadExpiresAtBefore(status, cutoff).stream().map(mapper::toRecord).toList();
    }

    @Override
    public List<DocumentRecord> findAllByStatus(DocumentStatus status) {
        return delegate.findAllByStatus(status).stream().map(mapper::toRecord).toList();
    }

    @Override
    public List<String> findTrackedStorageKeys() {
        return delegate.findTrackedStorageKeys();
    }

    @Override
    public DocumentRecord save(DocumentRecord document) {
        DocumentEntity entity = delegate.findById(document.id())
                .map(existing -> {
                    mapper.apply(existing, document);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(document));
        return mapper.toRecord(delegate.save(entity));
    }
}