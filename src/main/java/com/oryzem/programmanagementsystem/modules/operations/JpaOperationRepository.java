package com.oryzem.programmanagementsystem.modules.operations;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Transactional
public class JpaOperationRepository implements OperationRepository {

    private final SpringDataOperationJpaRepository delegate;

    public JpaOperationRepository(SpringDataOperationJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationRecord> findAll() {
        return delegate.findAll().stream()
                .map(OperationEntity::toDomain)
                .sorted(java.util.Comparator.comparing(OperationRecord::createdAt).thenComparing(OperationRecord::id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationRecord> findByTenantId(String tenantId) {
        return delegate.findByTenantIdOrderByCreatedAtAscIdAsc(tenantId).stream()
                .map(OperationEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperationRecord> findById(String operationId) {
        return delegate.findById(operationId).map(OperationEntity::toDomain);
    }

    @Override
    public OperationRecord save(OperationRecord operation) {
        return delegate.save(OperationEntity.fromDomain(operation)).toDomain();
    }

    @Override
    public void deleteById(String operationId) {
        delegate.deleteById(operationId);
    }

    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}
