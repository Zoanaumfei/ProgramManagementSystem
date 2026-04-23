package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectTemplateRepositoryAdapter implements ProjectTemplateRepository {

    private final SpringDataProjectTemplateJpaRepository delegate;
    private final ProjectTemplatePersistenceMapper mapper;

    public JpaProjectTemplateRepositoryAdapter(
            SpringDataProjectTemplateJpaRepository delegate,
            ProjectTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProjectTemplateAggregate> findByFrameworkTypeAndIsDefaultTrueAndStatus(String frameworkType, ProjectTemplateStatus status) {
        return delegate.findByFrameworkTypeAndIsDefaultTrueAndStatus(frameworkType, status)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProjectTemplateAggregate> findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(String structureTemplateId) {
        return delegate.findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(structureTemplateId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProjectTemplateAggregate> findAllByOrderByFrameworkTypeAscVersionDesc() {
        return delegate.findAllByOrderByFrameworkTypeAscVersionDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectTemplateAggregate save(ProjectTemplateAggregate projectTemplate) {
        if (delegate.existsById(projectTemplate.id())) {
            ProjectTemplateEntity entity = delegate.findById(projectTemplate.id()).orElseThrow();
            mapper.apply(entity, projectTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(projectTemplate)));
    }

    @Override
    public List<ProjectTemplateAggregate> saveAll(Iterable<ProjectTemplateAggregate> projectTemplates) {
        java.util.List<ProjectTemplateAggregate> saved = new java.util.ArrayList<>();
        for (ProjectTemplateAggregate projectTemplate : projectTemplates) {
            saved.add(save(projectTemplate));
        }
        return saved;
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
