package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectStructureNodePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectStructureNodeRepositoryAdapter implements ProjectStructureNodeRepository {

    private final SpringDataProjectStructureNodeJpaRepository delegate;
    private final ProjectStructureNodePersistenceMapper mapper;

    public JpaProjectStructureNodeRepositoryAdapter(
            SpringDataProjectStructureNodeJpaRepository delegate,
            ProjectStructureNodePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectStructureNodeAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProjectStructureNodeAggregate> findByIdAndProjectId(String id, String projectId) {
        return delegate.findByIdAndProjectId(id, projectId).map(mapper::toDomain);
    }

    @Override
    public List<ProjectStructureNodeAggregate> findAllByProjectIdOrderBySequenceNoAscIdAsc(String projectId) {
        return delegate.findAllByProjectIdOrderBySequenceNoAscIdAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectStructureNodeAggregate> findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(String projectId, String parentNodeId) {
        return delegate.findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(projectId, parentNodeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectStructureNodeAggregate save(ProjectStructureNodeAggregate node) {
        ProjectStructureNodeEntity entity = delegate.findById(node.id())
                .map(existing -> {
                    mapper.apply(existing, node);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(node));
        return mapper.toDomain(delegate.save(entity));
    }
}
