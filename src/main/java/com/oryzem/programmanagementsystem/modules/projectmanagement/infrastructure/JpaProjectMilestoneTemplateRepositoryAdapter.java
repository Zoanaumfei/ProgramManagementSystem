package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectMilestoneTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectMilestoneTemplateRepositoryAdapter implements ProjectMilestoneTemplateRepository {

    private final SpringDataProjectMilestoneTemplateJpaRepository delegate;
    private final ProjectMilestoneTemplatePersistenceMapper mapper;

    public JpaProjectMilestoneTemplateRepositoryAdapter(
            SpringDataProjectMilestoneTemplateJpaRepository delegate,
            ProjectMilestoneTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectMilestoneTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProjectMilestoneTemplateAggregate> findAllByTemplateIdOrderBySequenceNoAsc(String templateId) {
        return delegate.findAllByTemplateIdOrderBySequenceNoAsc(templateId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProjectMilestoneTemplateAggregate> findAllByTemplateIdInOrderByTemplateIdAscSequenceNoAsc(List<String> templateIds) {
        return delegate.findAllByTemplateIdInOrderByTemplateIdAscSequenceNoAsc(templateIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectMilestoneTemplateAggregate save(ProjectMilestoneTemplateAggregate milestoneTemplate) {
        if (delegate.existsById(milestoneTemplate.id())) {
            ProjectMilestoneTemplateEntity entity = delegate.findById(milestoneTemplate.id()).orElseThrow();
            mapper.apply(entity, milestoneTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(milestoneTemplate)));
    }
}
