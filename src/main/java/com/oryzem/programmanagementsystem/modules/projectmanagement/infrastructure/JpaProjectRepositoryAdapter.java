package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectRepositoryAdapter implements ProjectRepository {

    private final SpringDataProjectJpaRepository delegate;
    private final ProjectPersistenceMapper mapper;

    public JpaProjectRepositoryAdapter(SpringDataProjectJpaRepository delegate, ProjectPersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProjectAggregate> findByIdAndTenantId(String id, String tenantId) {
        return delegate.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndCodeIgnoreCase(String tenantId, String code) {
        return delegate.existsByTenantIdAndCodeIgnoreCase(tenantId, code);
    }

    @Override
    public boolean existsByTemplateId(String templateId) {
        return delegate.existsByTemplateId(templateId);
    }

    @Override
    public List<ProjectAggregate> findAllOrderByCreatedAtDescIdDesc() {
        return delegate.findAllByOrderByCreatedAtDescIdDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectAggregate> findAllByTenantIdOrderByCreatedAtDescIdDesc(String tenantId) {
        return delegate.findAllByTenantIdOrderByCreatedAtDescIdDesc(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectAggregate save(ProjectAggregate project) {
        ProjectEntity entity = delegate.findById(project.id())
                .map(existing -> {
                    mapper.apply(existing, project);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(project));
        return mapper.toDomain(delegate.save(entity));
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
