package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectMilestoneTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMilestoneTemplatePersistenceMapper {

    public ProjectMilestoneTemplateAggregate toDomain(ProjectMilestoneTemplateEntity entity) {
        return new ProjectMilestoneTemplateAggregate(
                entity.getId(),
                entity.getTemplateId(),
                entity.getPhaseTemplateId(),
                entity.getCode(),
                entity.getName(),
                entity.getSequenceNo(),
                entity.getDescription(),
                entity.getPlannedOffsetDays(),
                entity.getAppliesToType(),
                entity.getStructureLevelTemplateId(),
                entity.getOwnerOrganizationRole(),
                entity.getVisibilityScope());
    }

    public ProjectMilestoneTemplateEntity toNewEntity(ProjectMilestoneTemplateAggregate aggregate) {
        return ProjectMilestoneTemplateEntity.create(
                aggregate.id(),
                aggregate.templateId(),
                aggregate.phaseTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.sequenceNo(),
                aggregate.description(),
                aggregate.plannedOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.ownerOrganizationRole(),
                aggregate.visibilityScope());
    }

    public void apply(ProjectMilestoneTemplateEntity entity, ProjectMilestoneTemplateAggregate aggregate) {
        entity.applyUpdate(
                aggregate.phaseTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.plannedOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.ownerOrganizationRole(),
                aggregate.visibilityScope());
    }
}
