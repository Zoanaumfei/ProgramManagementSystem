package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.DeliverableTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliverableTemplatePersistenceMapper {

    public DeliverableTemplateAggregate toDomain(DeliverableTemplateEntity entity) {
        return new DeliverableTemplateAggregate(
                entity.getId(),
                entity.getTemplateId(),
                entity.getPhaseTemplateId(),
                entity.getMilestoneTemplateId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getDeliverableType(),
                entity.isRequiredDocument(),
                entity.getPlannedDueOffsetDays(),
                entity.getAppliesToType(),
                entity.getStructureLevelTemplateId(),
                entity.getResponsibleOrganizationRole(),
                entity.getApproverOrganizationRole(),
                entity.getVisibilityScope(),
                entity.getPriority());
    }

    public DeliverableTemplateEntity toNewEntity(DeliverableTemplateAggregate aggregate) {
        return DeliverableTemplateEntity.create(
                aggregate.id(),
                aggregate.templateId(),
                aggregate.phaseTemplateId(),
                aggregate.milestoneTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.requiredDocument(),
                aggregate.plannedDueOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.responsibleOrganizationRole(),
                aggregate.approverOrganizationRole(),
                aggregate.visibilityScope(),
                aggregate.priority());
    }

    public void apply(DeliverableTemplateEntity entity, DeliverableTemplateAggregate aggregate) {
        entity.applyUpdate(
                aggregate.phaseTemplateId(),
                aggregate.milestoneTemplateId(),
                aggregate.code(),
                aggregate.name(),
                aggregate.description(),
                aggregate.deliverableType(),
                aggregate.requiredDocument(),
                aggregate.plannedDueOffsetDays(),
                aggregate.appliesToType(),
                aggregate.structureLevelTemplateId(),
                aggregate.responsibleOrganizationRole(),
                aggregate.approverOrganizationRole(),
                aggregate.visibilityScope(),
                aggregate.priority());
    }
}
