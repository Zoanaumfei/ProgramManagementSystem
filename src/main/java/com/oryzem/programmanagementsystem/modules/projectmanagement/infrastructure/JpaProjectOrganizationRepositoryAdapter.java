package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectOrganizationPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectOrganizationRepositoryAdapter implements ProjectOrganizationRepository {

    private final SpringDataProjectOrganizationJpaRepository delegate;
    private final ProjectOrganizationPersistenceMapper mapper;

    public JpaProjectOrganizationRepositoryAdapter(
            SpringDataProjectOrganizationJpaRepository delegate,
            ProjectOrganizationPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<ProjectOrganizationAggregate> findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(String projectId) {
        return delegate.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ProjectOrganizationAggregate> findByProjectIdAndOrganizationIdAndActiveTrue(String projectId, String organizationId) {
        return delegate.findByProjectIdAndOrganizationIdAndActiveTrue(projectId, organizationId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByProjectIdAndOrganizationId(String projectId, String organizationId) {
        return delegate.existsByProjectIdAndOrganizationId(projectId, organizationId);
    }

    @Override
    public List<ProjectOrganizationAggregate> findAllByOrganizationIdAndActiveTrue(String organizationId) {
        return delegate.findAllByOrganizationIdAndActiveTrue(organizationId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectOrganizationAggregate save(ProjectOrganizationAggregate organization) {
        return mapper.toDomain(delegate.save(mapper.toNewEntity(organization)));
    }
}
