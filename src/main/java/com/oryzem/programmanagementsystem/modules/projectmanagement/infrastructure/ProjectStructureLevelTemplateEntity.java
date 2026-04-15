package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_structure_level_template")
public class ProjectStructureLevelTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "structure_template_id", length = 64, nullable = false)
    private String structureTemplateId;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(length = 64, nullable = false)
    private String code;
    @Column(name = "allows_children", nullable = false)
    private boolean allowsChildren;
    @Column(name = "allows_milestones", nullable = false)
    private boolean allowsMilestones;
    @Column(name = "allows_deliverables", nullable = false)
    private boolean allowsDeliverables;

    protected ProjectStructureLevelTemplateEntity() {}

    public static ProjectStructureLevelTemplateEntity create(
            String id,
            String structureTemplateId,
            int sequenceNo,
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
        ProjectStructureLevelTemplateEntity entity = new ProjectStructureLevelTemplateEntity();
        entity.id = id;
        entity.structureTemplateId = structureTemplateId;
        entity.sequenceNo = sequenceNo;
        entity.name = name;
        entity.code = code;
        entity.allowsChildren = allowsChildren;
        entity.allowsMilestones = allowsMilestones;
        entity.allowsDeliverables = allowsDeliverables;
        return entity;
    }

    public void applyUpdate(
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
        this.name = name;
        this.code = code;
        this.allowsChildren = allowsChildren;
        this.allowsMilestones = allowsMilestones;
        this.allowsDeliverables = allowsDeliverables;
    }

    public void changeSequence(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getId() { return id; }
    public String getStructureTemplateId() { return structureTemplateId; }
    public int getSequenceNo() { return sequenceNo; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isAllowsChildren() { return allowsChildren; }
    public boolean isAllowsMilestones() { return allowsMilestones; }
    public boolean isAllowsDeliverables() { return allowsDeliverables; }
}
