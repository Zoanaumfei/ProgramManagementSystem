package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectStructureLevelTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectStructureLevelTemplateRepositoryAdapter implements ProjectStructureLevelTemplateRepository {

    private final SpringDataProjectStructureLevelTemplateJpaRepository delegate;
    private final ProjectStructureLevelTemplatePersistenceMapper mapper;

    public JpaProjectStructureLevelTemplateRepositoryAdapter(
            SpringDataProjectStructureLevelTemplateJpaRepository delegate,
            ProjectStructureLevelTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectStructureLevelTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProjectStructureLevelTemplateAggregate> findAllByStructureTemplateIdOrderBySequenceNoAsc(String structureTemplateId) {
        return delegate.findAllByStructureTemplateIdOrderBySequenceNoAsc(structureTemplateId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectStructureLevelTemplateAggregate save(ProjectStructureLevelTemplateAggregate levelTemplate) {
        if (delegate.existsById(levelTemplate.id())) {
            ProjectStructureLevelTemplateEntity entity = delegate.findById(levelTemplate.id()).orElseThrow();
            mapper.apply(entity, levelTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(levelTemplate)));
    }

    @Override
    public List<ProjectStructureLevelTemplateAggregate> saveAll(Iterable<ProjectStructureLevelTemplateAggregate> levelTemplates) {
        java.util.List<ProjectStructureLevelTemplateAggregate> saved = new java.util.ArrayList<>();
        for (ProjectStructureLevelTemplateAggregate levelTemplate : levelTemplates) {
            saved.add(save(levelTemplate));
        }
        return saved;
    }

    @Override
    public List<ProjectStructureLevelTemplateAggregate> saveAllAndFlush(Iterable<ProjectStructureLevelTemplateAggregate> levelTemplates) {
        java.util.List<ProjectStructureLevelTemplateAggregate> saved = new java.util.ArrayList<>();
        for (ProjectStructureLevelTemplateAggregate levelTemplate : levelTemplates) {
            if (delegate.existsById(levelTemplate.id())) {
                ProjectStructureLevelTemplateEntity entity = delegate.findById(levelTemplate.id()).orElseThrow();
                mapper.apply(entity, levelTemplate);
                saved.add(mapper.toDomain(entity));
            } else {
                saved.add(levelTemplate);
            }
        }
        delegate.flush();
        return saved;
    }
}
