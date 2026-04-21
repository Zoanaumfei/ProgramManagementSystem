package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.model.DocumentBindingRecord;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.mapper.DocumentBindingPersistenceMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDocumentBindingRepositoryAdapter implements DocumentBindingRepository {

    private final SpringDataDocumentBindingJpaRepository delegate;
    private final DocumentBindingPersistenceMapper mapper;

    public JpaDocumentBindingRepositoryAdapter(
            SpringDataDocumentBindingJpaRepository delegate,
            DocumentBindingPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<DocumentBindingRecord> findByDocumentId(String documentId) {
        return delegate.findByDocumentId(documentId).map(mapper::toRecord);
    }

    @Override
    public List<DocumentBindingRecord> findAllByContextTypeAndContextIdOrderByCreatedAtDesc(
            DocumentContextType contextType,
            String contextId) {
        return delegate.findAllByContextTypeAndContextIdOrderByCreatedAtDesc(contextType, contextId).stream()
                .map(mapper::toRecord)
                .toList();
    }

    @Override
    public long countTrackedByContext(DocumentContextType contextType, String contextId) {
        return delegate.countTrackedByContext(contextType.name(), contextId);
    }

    @Override
    public List<DocumentBindingRecord> findAll() {
        return delegate.findAll().stream().map(mapper::toRecord).toList();
    }

    @Override
    public DocumentBindingRecord save(DocumentBindingRecord binding) {
        return mapper.toRecord(delegate.save(mapper.toNewEntity(binding)));
    }

    @Override
    public void deleteAllByDocumentIdIn(Collection<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        delegate.deleteAllByDocumentIdIn(documentIds);
    }
}
