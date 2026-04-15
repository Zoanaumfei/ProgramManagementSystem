package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectOrganizationEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectOrganizationPersistenceMapper {

    public ProjectOrganizationAggregate toDomain(ProjectOrganizationEntity entity) {
        return new ProjectOrganizationAggregate(
                entity.getId(),
                entity.getProjectId(),
                entity.getOrganizationId(),
                entity.getRoleType(),
                entity.getJoinedAt(),
                entity.isActive());
    }

    public ProjectOrganizationEntity toNewEntity(ProjectOrganizationAggregate aggregate) {
        return ProjectOrganizationEntity.create(
                aggregate.id(),
                aggregate.projectId(),
                aggregate.organizationId(),
                aggregate.roleType(),
                aggregate.joinedAt());
    }
}
