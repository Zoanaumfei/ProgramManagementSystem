package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectDeliverableEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDeliverablePersistenceMapper {

    public ProjectDeliverableAggregate toDomain(ProjectDeliverableEntity entity) {
        return entity.toDomain();
    }

    public ProjectDeliverableEntity toNewEntity(ProjectDeliverableAggregate aggregate) {
        return ProjectDeliverableEntity.create(aggregate);
    }

    public void apply(ProjectDeliverableEntity entity, ProjectDeliverableAggregate aggregate) {
        entity.apply(aggregate);
    }
}
