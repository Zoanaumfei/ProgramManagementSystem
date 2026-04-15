package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectPhaseTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectPhaseTemplatePersistenceMapper {

    public ProjectPhaseTemplateAggregate toDomain(ProjectPhaseTemplateEntity entity) {
        return new ProjectPhaseTemplateAggregate(
                entity.getId(),
                entity.getTemplateId(),
                entity.getSequenceNo(),
                entity.getName(),
                entity.getDescription(),
                entity.getPlannedStartOffsetDays(),
                entity.getPlannedEndOffsetDays());
    }

    public ProjectPhaseTemplateEntity toNewEntity(ProjectPhaseTemplateAggregate aggregate) {
        return ProjectPhaseTemplateEntity.create(
                aggregate.id(),
                aggregate.templateId(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.description(),
                aggregate.plannedStartOffsetDays(),
                aggregate.plannedEndOffsetDays());
    }

    public void apply(ProjectPhaseTemplateEntity entity, ProjectPhaseTemplateAggregate aggregate) {
        entity.applyUpdate(
                aggregate.name(),
                aggregate.description(),
                aggregate.plannedStartOffsetDays(),
                aggregate.plannedEndOffsetDays());
    }
}
