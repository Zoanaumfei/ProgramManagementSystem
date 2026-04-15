package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectPhaseTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectPhaseTemplateRepositoryAdapter implements ProjectPhaseTemplateRepository {

    private final SpringDataProjectPhaseTemplateJpaRepository delegate;
    private final ProjectPhaseTemplatePersistenceMapper mapper;

    public JpaProjectPhaseTemplateRepositoryAdapter(
            SpringDataProjectPhaseTemplateJpaRepository delegate,
            ProjectPhaseTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectPhaseTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProjectPhaseTemplateAggregate> findAllByTemplateIdOrderBySequenceNoAsc(String templateId) {
        return delegate.findAllByTemplateIdOrderBySequenceNoAsc(templateId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectPhaseTemplateAggregate save(ProjectPhaseTemplateAggregate phaseTemplate) {
        if (delegate.existsById(phaseTemplate.id())) {
            ProjectPhaseTemplateEntity entity = delegate.findById(phaseTemplate.id()).orElseThrow();
            mapper.apply(entity, phaseTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(phaseTemplate)));
    }
}
