package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectPhaseEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectPhasePersistenceMapper {

    public ProjectPhaseAggregate toDomain(ProjectPhaseEntity entity) {
        return new ProjectPhaseAggregate(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getSequenceNo(),
                entity.getStatus(),
                entity.getPlannedStartDate(),
                entity.getPlannedEndDate(),
                entity.getActualStartDate(),
                entity.getActualEndDate(),
                entity.getVersion());
    }

    public ProjectPhaseEntity toNewEntity(ProjectPhaseAggregate aggregate) {
        return ProjectPhaseEntity.create(
                aggregate.id(),
                aggregate.projectId(),
                aggregate.name(),
                aggregate.sequenceNo(),
                aggregate.plannedStartDate(),
                aggregate.plannedEndDate());
    }
}
