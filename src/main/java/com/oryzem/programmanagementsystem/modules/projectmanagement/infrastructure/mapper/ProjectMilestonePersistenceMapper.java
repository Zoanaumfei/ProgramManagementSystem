package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectMilestoneEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMilestonePersistenceMapper {

    public ProjectMilestoneAggregate toDomain(ProjectMilestoneEntity entity) {
        return entity.toDomain();
    }

    public ProjectMilestoneEntity toNewEntity(ProjectMilestoneAggregate aggregate) {
        return ProjectMilestoneEntity.create(aggregate);
    }

    public void apply(ProjectMilestoneEntity entity, ProjectMilestoneAggregate aggregate) {
        entity.apply(aggregate);
    }
}
