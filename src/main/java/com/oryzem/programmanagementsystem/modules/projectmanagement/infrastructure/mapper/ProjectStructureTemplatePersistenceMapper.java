package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectStructureTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectStructureTemplatePersistenceMapper {

    public ProjectStructureTemplateAggregate toDomain(ProjectStructureTemplateEntity entity) {
        return new ProjectStructureTemplateAggregate(
                entity.getId(),
                entity.getName(),
                entity.getFrameworkType(),
                entity.getVersion(),
                entity.isActive(),
                entity.getOwnerOrganizationId(),
                entity.getCreatedAt());
    }

    public ProjectStructureTemplateEntity toNewEntity(ProjectStructureTemplateAggregate aggregate) {
        return ProjectStructureTemplateEntity.create(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.active(),
                aggregate.ownerOrganizationId(),
                aggregate.createdAt());
    }

    public void apply(ProjectStructureTemplateEntity entity, ProjectStructureTemplateAggregate aggregate) {
        entity.rename(aggregate.name());
        entity.setActive(aggregate.active());
    }
}
