package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectPhasePersistenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectPhaseRepositoryAdapter implements ProjectPhaseRepository {

    private final SpringDataProjectPhaseJpaRepository delegate;
    private final ProjectPhasePersistenceMapper mapper;

    public JpaProjectPhaseRepositoryAdapter(
            SpringDataProjectPhaseJpaRepository delegate,
            ProjectPhasePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<ProjectPhaseAggregate> findAllByProjectIdOrderBySequenceNoAsc(String projectId) {
        return delegate.findAllByProjectIdOrderBySequenceNoAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectPhaseAggregate save(ProjectPhaseAggregate phase) {
        return mapper.toDomain(delegate.save(mapper.toNewEntity(phase)));
    }
}
