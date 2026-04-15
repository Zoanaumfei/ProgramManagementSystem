package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectStructureLevelTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectStructureLevelTemplatePersistenceMapper {

    public ProjectStructureLevelTemplateAggregate toDomain(ProjectStructureLevelTemplateEntity entity) {
        return new ProjectStructureLevelTemplateAggregate(
                entity.getId(),
                entity.getStructureTemplateId(),
                entity.getSequenceNo(),
                entity.getName(),
                entity.getCode(),
                entity.isAllowsChildren(),
                entity.isAllowsMilestones(),
                entity.isAllowsDeliverables());
    }

    public ProjectStructureLevelTemplateEntity toNewEntity(ProjectStructureLevelTemplateAggregate aggregate) {
        return ProjectStructureLevelTemplateEntity.create(
                aggregate.id(),
                aggregate.structureTemplateId(),
                aggregate.sequenceNo(),
                aggregate.name(),
                aggregate.code(),
                aggregate.allowsChildren(),
                aggregate.allowsMilestones(),
                aggregate.allowsDeliverables());
    }

    public void apply(ProjectStructureLevelTemplateEntity entity, ProjectStructureLevelTemplateAggregate aggregate) {
        entity.applyUpdate(
                aggregate.name(),
                aggregate.code(),
                aggregate.allowsChildren(),
                aggregate.allowsMilestones(),
                aggregate.allowsDeliverables());
        if (entity.getSequenceNo() != aggregate.sequenceNo()) {
            entity.changeSequence(aggregate.sequenceNo());
        }
    }
}
