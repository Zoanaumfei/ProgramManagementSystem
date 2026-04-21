package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectTemplatePersistenceMapper {

    public ProjectTemplateAggregate toDomain(ProjectTemplateEntity entity) {
        return new ProjectTemplateAggregate(
                entity.getId(),
                entity.getName(),
                entity.getFrameworkType(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getStructureTemplateId(),
                entity.getOwnerOrganizationId(),
                entity.isDefault(),
                entity.getCreatedAt());
    }

    public ProjectTemplateEntity toNewEntity(ProjectTemplateAggregate aggregate) {
        return ProjectTemplateEntity.create(
                aggregate.id(),
                aggregate.name(),
                aggregate.frameworkType(),
                aggregate.version(),
                aggregate.status(),
                aggregate.structureTemplateId(),
                aggregate.ownerOrganizationId(),
                aggregate.isDefault(),
                aggregate.createdAt());
    }

    public void apply(ProjectTemplateEntity entity, ProjectTemplateAggregate aggregate) {
        entity.applyUpdate(
                aggregate.name(),
                aggregate.status(),
                aggregate.structureTemplateId(),
                aggregate.isDefault());
    }
}
