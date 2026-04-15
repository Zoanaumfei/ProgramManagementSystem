package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(name = "project_phase")
public class ProjectPhaseEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;
    @Column(length = 160, nullable = false)
    private String name;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
    @Column(length = 32, nullable = false)
    private String status;
    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;
    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectPhaseEntity() {}

    public static ProjectPhaseEntity create(String id, String projectId, String name, int sequenceNo, LocalDate plannedStartDate, LocalDate plannedEndDate) {
        ProjectPhaseEntity entity = new ProjectPhaseEntity();
        entity.id = id;
        entity.projectId = projectId;
        entity.name = name;
        entity.sequenceNo = sequenceNo;
        entity.status = "PLANNED";
        entity.plannedStartDate = plannedStartDate;
        entity.plannedEndDate = plannedEndDate;
        return entity;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public int getSequenceNo() { return sequenceNo; }
    public String getStatus() { return status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public long getVersion() { return version; }
}
