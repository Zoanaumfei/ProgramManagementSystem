package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
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
    private final AccessContextService accessContextService;

    public JpaOperationRepository(
            SpringDataOperationJpaRepository delegate,
            AccessContextService accessContextService) {
        this.delegate = delegate;
        this.accessContextService = accessContextService;
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
        return delegate.findByTenantIdInOrderByCreatedAtAscIdAsc(accessContextService.equivalentTenantIds(tenantId)).stream()
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
        return delegate.save(OperationEntity.fromDomain(new OperationRecord(
                                operation.id(),
                                operation.title(),
                                operation.description(),
                                accessContextService.canonicalTenantId(operation.tenantId()),
                                operation.tenantType(),
                                operation.createdBy(),
                                operation.status(),
                                operation.createdAt(),
                                operation.updatedAt(),
                                operation.submittedAt(),
                                operation.approvedAt(),
                                operation.rejectedAt(),
                                operation.reopenedAt(),
                                operation.reprocessedAt())))
                        .toDomain();
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
