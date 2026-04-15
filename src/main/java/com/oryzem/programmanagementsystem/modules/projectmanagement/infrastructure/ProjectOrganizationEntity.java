package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_organization")
public class ProjectOrganizationEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(name = "organization_id", length = 64, nullable = false)
    private String organizationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", length = 32, nullable = false)
    private ProjectOrganizationRoleType roleType;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(nullable = false)
    private boolean active;

    protected ProjectOrganizationEntity() {}

    public static ProjectOrganizationEntity create(String id, String projectId, String organizationId, ProjectOrganizationRoleType roleType, Instant joinedAt) {
        ProjectOrganizationEntity entity = new ProjectOrganizationEntity();
        entity.id = id;
        entity.projectId = projectId;
        entity.organizationId = organizationId;
        entity.roleType = roleType;
        entity.joinedAt = joinedAt;
        entity.active = true;
        return entity;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getOrganizationId() { return organizationId; }
    public ProjectOrganizationRoleType getRoleType() { return roleType; }
    public Instant getJoinedAt() { return joinedAt; }
    public boolean isActive() { return active; }
}
