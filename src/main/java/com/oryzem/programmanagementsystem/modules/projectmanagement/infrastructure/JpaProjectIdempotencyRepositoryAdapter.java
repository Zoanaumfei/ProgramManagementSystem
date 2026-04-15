package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.ProjectIdempotencyRecord;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectIdempotencyRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectIdempotencyPersistenceMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectIdempotencyRepositoryAdapter implements ProjectIdempotencyRepository {

    private final SpringDataProjectIdempotencyJpaRepository delegate;
    private final ProjectIdempotencyPersistenceMapper mapper;

    public JpaProjectIdempotencyRepositoryAdapter(
            SpringDataProjectIdempotencyJpaRepository delegate,
            ProjectIdempotencyPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectIdempotencyRecord> findByIdempotencyKey(String idempotencyKey, String tenantId, String operation) {
        return delegate.findById(new ProjectIdempotencyEntity.ProjectIdempotencyId(idempotencyKey, tenantId, operation))
                .map(mapper::toRecord);
    }

    @Override
    public ProjectIdempotencyRecord save(ProjectIdempotencyRecord record) {
        return mapper.toRecord(delegate.save(mapper.toEntity(record)));
    }
}
