package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectStructureNodeEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectStructureNodePersistenceMapper {

    public ProjectStructureNodeAggregate toDomain(ProjectStructureNodeEntity entity) {
        return new ProjectStructureNodeAggregate(
                entity.getId(),
                entity.getProjectId(),
                entity.getLevelTemplateId(),
                entity.getParentNodeId(),
                entity.getName(),
                entity.getCode(),
                entity.getSequenceNo(),
                entity.getOwnerOrganizationId(),
                entity.getResponsibleUserId(),
                entity.getStatus(),
                entity.getVisibilityScope(),
                entity.getVersion());
    }

    public ProjectStructureNodeEntity toNewEntity(ProjectStructureNodeAggregate aggregate) {
        if (aggregate.parentNodeId() == null) {
            return ProjectStructureNodeEntity.createRoot(
                    aggregate.projectId(),
                    aggregate.levelTemplateId(),
                    aggregate.name(),
                    aggregate.code(),
                    aggregate.ownerOrganizationId(),
                    aggregate.responsibleUserId(),
                    aggregate.status(),
                    aggregate.visibilityScope());
        }
        return ProjectStructureNodeEntity.create(
                aggregate.id(),
                aggregate.projectId(),
                aggregate.levelTemplateId(),
                aggregate.parentNodeId(),
                aggregate.name(),
                aggregate.code(),
                aggregate.sequenceNo(),
                aggregate.ownerOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.status(),
                aggregate.visibilityScope());
    }

    public void apply(ProjectStructureNodeEntity entity, ProjectStructureNodeAggregate aggregate) {
        entity.applyUpdate(
                aggregate.name(),
                aggregate.code(),
                aggregate.ownerOrganizationId(),
                aggregate.responsibleUserId(),
                aggregate.visibilityScope());
        if ((entity.getParentNodeId() == null && aggregate.parentNodeId() != null)
                || (entity.getParentNodeId() != null && !entity.getParentNodeId().equals(aggregate.parentNodeId()))
                || entity.getSequenceNo() != aggregate.sequenceNo()) {
            entity.moveTo(aggregate.parentNodeId(), aggregate.sequenceNo());
        }
    }
}
