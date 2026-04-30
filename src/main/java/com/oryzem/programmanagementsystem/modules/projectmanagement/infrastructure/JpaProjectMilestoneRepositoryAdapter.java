package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectMilestonePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectMilestoneRepositoryAdapter implements ProjectMilestoneRepository {

    private final SpringDataProjectMilestoneJpaRepository delegate;
    private final ProjectMilestonePersistenceMapper mapper;

    public JpaProjectMilestoneRepositoryAdapter(
            SpringDataProjectMilestoneJpaRepository delegate,
            ProjectMilestonePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectMilestoneAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProjectMilestoneAggregate> findByIdAndProjectId(String id, String projectId) {
        return delegate.findByIdAndProjectId(id, projectId).map(mapper::toDomain);
    }

    @Override
    public List<ProjectMilestoneAggregate> findAllByProjectIdOrderBySequenceNoAsc(String projectId) {
        return delegate.findAllByProjectIdOrderBySequenceNoAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectMilestoneAggregate> findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(String projectId, String structureNodeId) {
        return delegate.findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(projectId, structureNodeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectMilestoneAggregate save(ProjectMilestoneAggregate milestone) {
        ProjectMilestoneEntity entity = delegate.findById(milestone.id())
                .map(existing -> {
                    mapper.apply(existing, milestone);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(milestone));
        return mapper.toDomain(delegate.save(entity));
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
