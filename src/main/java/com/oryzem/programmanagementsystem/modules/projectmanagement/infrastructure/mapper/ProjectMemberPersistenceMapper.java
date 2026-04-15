package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.mapper;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectMemberEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberPersistenceMapper {

    public ProjectMemberAggregate toDomain(ProjectMemberEntity entity) {
        return new ProjectMemberAggregate(
                entity.getId(),
                entity.getProjectId(),
                entity.getUserId(),
                entity.getOrganizationId(),
                entity.getProjectRole(),
                entity.isActive(),
                entity.getAssignedAt());
    }

    public ProjectMemberEntity toNewEntity(ProjectMemberAggregate aggregate) {
        return ProjectMemberEntity.create(
                aggregate.id(),
                aggregate.projectId(),
                aggregate.userId(),
                aggregate.organizationId(),
                aggregate.projectRole(),
                aggregate.assignedAt());
    }
}
