package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectMemberPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectMemberRepositoryAdapter implements ProjectMemberRepository {

    private final SpringDataProjectMemberJpaRepository delegate;
    private final ProjectMemberPersistenceMapper mapper;

    public JpaProjectMemberRepositoryAdapter(
            SpringDataProjectMemberJpaRepository delegate,
            ProjectMemberPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<ProjectMemberAggregate> findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(String projectId) {
        return delegate.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ProjectMemberAggregate> findByProjectIdAndUserIdAndActiveTrue(String projectId, String userId) {
        return delegate.findByProjectIdAndUserIdAndActiveTrue(projectId, userId).map(mapper::toDomain);
    }

    @Override
    public List<ProjectMemberAggregate> findAllByProjectIdAndUserId(String projectId, String userId) {
        return delegate.findAllByProjectIdAndUserId(projectId, userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectMemberAggregate> findAllByUserIdAndActiveTrue(String userId) {
        return delegate.findAllByUserIdAndActiveTrue(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectMemberAggregate save(ProjectMemberAggregate member) {
        return mapper.toDomain(delegate.save(mapper.toNewEntity(member)));
    }
}
