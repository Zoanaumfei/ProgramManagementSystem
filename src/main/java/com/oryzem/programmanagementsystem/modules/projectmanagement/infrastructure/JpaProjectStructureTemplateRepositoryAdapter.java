package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper.ProjectStructureTemplatePersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectStructureTemplateRepositoryAdapter implements ProjectStructureTemplateRepository {

    private final SpringDataProjectStructureTemplateJpaRepository delegate;
    private final ProjectStructureTemplatePersistenceMapper mapper;

    public JpaProjectStructureTemplateRepositoryAdapter(
            SpringDataProjectStructureTemplateJpaRepository delegate,
            ProjectStructureTemplatePersistenceMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProjectStructureTemplateAggregate> findById(String id) {
        return delegate.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProjectStructureTemplateAggregate> findAllByOrderByFrameworkTypeAscVersionDescNameAsc() {
        return delegate.findAllByOrderByFrameworkTypeAscVersionDescNameAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProjectStructureTemplateAggregate> findAllByActiveTrueOrderByFrameworkTypeAscVersionDescNameAsc() {
        return delegate.findAllByActiveTrueOrderByFrameworkTypeAscVersionDescNameAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ProjectStructureTemplateAggregate save(ProjectStructureTemplateAggregate structureTemplate) {
        if (delegate.existsById(structureTemplate.id())) {
            ProjectStructureTemplateEntity entity = delegate.findById(structureTemplate.id()).orElseThrow();
            mapper.apply(entity, structureTemplate);
            return mapper.toDomain(delegate.save(entity));
        }
        return mapper.toDomain(delegate.save(mapper.toNewEntity(structureTemplate)));
    }
}
