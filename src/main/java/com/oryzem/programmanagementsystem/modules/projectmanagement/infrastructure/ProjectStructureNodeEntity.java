package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "project_structure_node")
public class ProjectStructureNodeEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(name = "level_template_id", length = 64, nullable = false)
    private String levelTemplateId;
    @Column(name = "parent_node_id", length = 64)
    private String parentNodeId;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(name = "owner_organization_id", length = 64)
    private String ownerOrganizationId;
    @Column(name = "responsible_user_id", length = 64)
    private String responsibleUserId;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectStructureNodeStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", length = 32, nullable = false)
    private ProjectVisibilityScope visibilityScope;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectStructureNodeEntity() {}

    public static ProjectStructureNodeEntity createRoot(
            String projectId,
            String levelTemplateId,
            String name,
            String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectStructureNodeStatus status,
            ProjectVisibilityScope visibilityScope) {
        ProjectStructureNodeEntity entity = new ProjectStructureNodeEntity();
        entity.id = ProjectIds.rootProjectStructureNodeId(projectId);
        entity.projectId = projectId;
        entity.levelTemplateId = levelTemplateId;
        entity.parentNodeId = null;
        entity.name = name;
        entity.code = code;
        entity.sequenceNo = 1;
        entity.ownerOrganizationId = ownerOrganizationId;
        entity.responsibleUserId = responsibleUserId;
        entity.status = status;
        entity.visibilityScope = visibilityScope;
        entity.version = 0L;
        return entity;
    }

    public static ProjectStructureNodeEntity create(
            String id,
            String projectId,
            String levelTemplateId,
            String parentNodeId,
            String name,
            String code,
            int sequenceNo,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectStructureNodeStatus status,
            ProjectVisibilityScope visibilityScope) {
        ProjectStructureNodeEntity entity = new ProjectStructureNodeEntity();
        entity.id = id;
        entity.projectId = projectId;
        entity.levelTemplateId = levelTemplateId;
        entity.parentNodeId = parentNodeId;
        entity.name = name;
        entity.code = code;
        entity.sequenceNo = sequenceNo;
        entity.ownerOrganizationId = ownerOrganizationId;
        entity.responsibleUserId = responsibleUserId;
        entity.status = status;
        entity.visibilityScope = visibilityScope;
        entity.version = 0L;
        return entity;
    }

    public void applyUpdate(
            String name,
            String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectVisibilityScope visibilityScope) {
        this.name = name;
        this.code = code;
        this.ownerOrganizationId = ownerOrganizationId;
        this.responsibleUserId = responsibleUserId;
        this.visibilityScope = visibilityScope;
    }

    public void moveTo(String parentNodeId, int sequenceNo) {
        this.parentNodeId = parentNodeId;
        this.sequenceNo = sequenceNo;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getLevelTemplateId() { return levelTemplateId; }
    public String getParentNodeId() { return parentNodeId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public int getSequenceNo() { return sequenceNo; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public String getResponsibleUserId() { return responsibleUserId; }
    public ProjectStructureNodeStatus getStatus() { return status; }
    public ProjectVisibilityScope getVisibilityScope() { return visibilityScope; }
    public long getVersion() { return version; }
}
