package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_phase_template")
public class ProjectPhaseTemplateEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "planned_start_offset_days")
    private Integer plannedStartOffsetDays;
    @Column(name = "planned_end_offset_days", nullable = false)
    private int plannedEndOffsetDays;

    protected ProjectPhaseTemplateEntity() {}

    public static ProjectPhaseTemplateEntity create(
            String id,
            String templateId,
            int sequenceNo,
            String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
        ProjectPhaseTemplateEntity entity = new ProjectPhaseTemplateEntity();
        entity.id = id;
        entity.templateId = templateId;
        entity.sequenceNo = sequenceNo;
        entity.name = name;
        entity.description = description;
        entity.plannedStartOffsetDays = plannedStartOffsetDays;
        entity.plannedEndOffsetDays = plannedEndOffsetDays;
        return entity;
    }

    public void applyUpdate(
            String name,
            String description,
            Integer plannedStartOffsetDays,
            int plannedEndOffsetDays) {
        this.name = name;
        this.description = description;
        this.plannedStartOffsetDays = plannedStartOffsetDays;
        this.plannedEndOffsetDays = plannedEndOffsetDays;
    }

    public String getId() { return id; }
    public String getTemplateId() { return templateId; }
    public int getSequenceNo() { return sequenceNo; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getPlannedStartOffsetDays() { return plannedStartOffsetDays; }
    public int getPlannedEndOffsetDays() { return plannedEndOffsetDays; }
}
