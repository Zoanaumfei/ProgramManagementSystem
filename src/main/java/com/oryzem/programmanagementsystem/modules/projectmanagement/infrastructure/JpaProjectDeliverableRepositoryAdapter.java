package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectDeliverablePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectDeliverableRepositoryAdapter implements ProjectDeliverableRepository {

    private final SpringDataProjectDeliverableJpaRepository delegate;
    private final ProjectDeliverablePersistenceMapper mapper;

    public JpaProjectDeliverableRepositoryAdapter(
            SpringDataProjectDeliverableJpaRepository delegate,
            ProjectDeliverablePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectDeliverableAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProjectDeliverableAggregate> findByIdAndProjectId(String id, String projectId) {
        return delegate.findByIdAndProjectId(id, projectId).map(mapper::toDomain);
    }

    @Override
    public List<ProjectDeliverableAggregate> findAllByProjectIdOrderByPlannedDueDateAscIdAsc(String projectId) {
        return delegate.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectDeliverableAggregate> findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(String projectId, String structureNodeId) {
        return delegate.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(projectId, structureNodeId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectDeliverableAggregate save(ProjectDeliverableAggregate deliverable) {
        ProjectDeliverableEntity entity = delegate.findById(deliverable.id())
                .map(existing -> {
                    mapper.apply(existing, deliverable);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(deliverable));
        return mapper.toDomain(delegate.save(entity));
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
