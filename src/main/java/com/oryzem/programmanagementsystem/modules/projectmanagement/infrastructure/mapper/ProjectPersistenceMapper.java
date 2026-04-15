package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectPersistenceMapper {

    public ProjectAggregate toDomain(ProjectEntity entity) {
        return entity.toDomain();
    }

    public ProjectEntity toNewEntity(ProjectAggregate aggregate) {
        return ProjectEntity.create(aggregate);
    }

    public void apply(ProjectEntity entity, ProjectAggregate aggregate) {
        entity.apply(aggregate);
    }
}
