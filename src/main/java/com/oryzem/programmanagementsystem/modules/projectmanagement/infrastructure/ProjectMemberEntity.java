package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_member")
public class ProjectMemberEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;
    @Column(name = "organization_id", length = 64, nullable = false)
    private String organizationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", length = 32, nullable = false)
    private ProjectMemberRole projectRole;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected ProjectMemberEntity() {}

    public static ProjectMemberEntity create(String id, String projectId, String userId, String organizationId, ProjectMemberRole projectRole, Instant assignedAt) {
        ProjectMemberEntity entity = new ProjectMemberEntity();
        entity.id = id;
        entity.projectId = projectId;
        entity.userId = userId;
        entity.organizationId = organizationId;
        entity.projectRole = projectRole;
        entity.active = true;
        entity.assignedAt = assignedAt;
        return entity;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public String getOrganizationId() { return organizationId; }
    public ProjectMemberRole getProjectRole() { return projectRole; }
    public boolean isActive() { return active; }
    public Instant getAssignedAt() { return assignedAt; }
}
