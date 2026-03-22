package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "milestone_template")
class MilestoneTemplateEntity extends JpaPortfolioAuditableEntity {

    private String name;
    private String description;
    private MilestoneTemplateStatus status;
    private List<MilestoneTemplateItemEntity> items = new ArrayList<>();

    protected MilestoneTemplateEntity() {
    }

    static MilestoneTemplateEntity create(
            String actor,
            String name,
            String description,
            MilestoneTemplateStatus status) {
        MilestoneTemplateEntity template = new MilestoneTemplateEntity();
        template.initialize(PortfolioIds.newId("MTP"), actor);
        template.name = name;
        template.description = description;
        template.status = status;
        return template;
    }

    void addItem(MilestoneTemplateItemEntity item, String actor) {
        items.add(item);
        touch(actor);
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(columnDefinition = "TEXT")
    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public MilestoneTemplateStatus getStatus() {
        return status;
    }

    protected void setStatus(MilestoneTemplateStatus status) {
        this.status = status;
    }

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<MilestoneTemplateItemEntity> getItems() {
        return items;
    }

    protected void setItems(List<MilestoneTemplateItemEntity> items) {
        this.items = items;
    }
}

@Entity
@Table(name = "milestone_template_item")
class MilestoneTemplateItemEntity extends JpaPortfolioAuditableEntity {

    private MilestoneTemplateEntity template;
    private String name;
    private int sortOrder;
    private boolean required;
    private Integer offsetWeeks;

    protected MilestoneTemplateItemEntity() {
    }

    static MilestoneTemplateItemEntity create(
            String actor,
            MilestoneTemplateEntity template,
            String name,
            int sortOrder,
            boolean required,
            Integer offsetWeeks) {
        MilestoneTemplateItemEntity item = new MilestoneTemplateItemEntity();
        item.initialize(PortfolioIds.newId("MTI"), actor);
        item.template = template;
        item.name = name;
        item.sortOrder = sortOrder;
        item.required = required;
        item.offsetWeeks = offsetWeeks;
        return item;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    public MilestoneTemplateEntity getTemplate() {
        return template;
    }

    protected void setTemplate(MilestoneTemplateEntity template) {
        this.template = template;
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(name = "sort_order", nullable = false)
    public int getSortOrder() {
        return sortOrder;
    }

    protected void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Column(name = "required_flag", nullable = false)
    public boolean isRequired() {
        return required;
    }

    protected void setRequired(boolean required) {
        this.required = required;
    }

    @Column(name = "offset_weeks")
    public Integer getOffsetWeeks() {
        return offsetWeeks;
    }

    protected void setOffsetWeeks(Integer offsetWeeks) {
        this.offsetWeeks = offsetWeeks;
    }
}

@Entity
@Table(name = "project_milestone")
class ProjectMilestoneEntity extends JpaPortfolioAuditableEntity {

    private ProjectEntity project;
    private String milestoneTemplateItemId;
    private String name;
    private int sortOrder;
    private ProjectMilestoneStatus status;
    private LocalDate plannedDate;
    private LocalDate actualDate;

    protected ProjectMilestoneEntity() {
    }

    static ProjectMilestoneEntity create(
            String actor,
            ProjectEntity project,
            String milestoneTemplateItemId,
            String name,
            int sortOrder,
            ProjectMilestoneStatus status,
            LocalDate plannedDate) {
        ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
        milestone.initialize(PortfolioIds.newId("PMS"), actor);
        milestone.project = project;
        milestone.milestoneTemplateItemId = milestoneTemplateItemId;
        milestone.name = name;
        milestone.sortOrder = sortOrder;
        milestone.status = status;
        milestone.plannedDate = plannedDate;
        return milestone;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public ProjectEntity getProject() {
        return project;
    }

    protected void setProject(ProjectEntity project) {
        this.project = project;
    }

    @Column(name = "milestone_template_item_id", length = 64)
    public String getMilestoneTemplateItemId() {
        return milestoneTemplateItemId;
    }

    protected void setMilestoneTemplateItemId(String milestoneTemplateItemId) {
        this.milestoneTemplateItemId = milestoneTemplateItemId;
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(name = "sort_order", nullable = false)
    public int getSortOrder() {
        return sortOrder;
    }

    protected void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public ProjectMilestoneStatus getStatus() {
        return status;
    }

    protected void setStatus(ProjectMilestoneStatus status) {
        this.status = status;
    }

    @Column(name = "planned_date", nullable = false)
    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    protected void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    @Column(name = "actual_date")
    public LocalDate getActualDate() {
        return actualDate;
    }

    protected void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }
}

@Entity
@Table(name = "open_issue")
class OpenIssueEntity extends JpaPortfolioAuditableEntity {

    private ProgramEntity program;
    private String title;
    private String description;
    private OpenIssueStatus status;
    private OpenIssueSeverity severity;
    private OffsetDateTime openedAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;

    protected OpenIssueEntity() {
    }

    static OpenIssueEntity create(
            String actor,
            ProgramEntity program,
            String title,
            String description,
            OpenIssueStatus status,
            OpenIssueSeverity severity,
            OffsetDateTime openedAt) {
        OpenIssueEntity issue = new OpenIssueEntity();
        issue.initialize(PortfolioIds.newId("OPI"), actor);
        issue.program = program;
        issue.title = title;
        issue.description = description;
        issue.status = status;
        issue.severity = severity;
        issue.openedAt = openedAt;
        return issue;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    public ProgramEntity getProgram() {
        return program;
    }

    protected void setProgram(ProgramEntity program) {
        this.program = program;
    }

    @Column(length = 160, nullable = false)
    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    @Column(columnDefinition = "TEXT")
    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public OpenIssueStatus getStatus() {
        return status;
    }

    protected void setStatus(OpenIssueStatus status) {
        this.status = status;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public OpenIssueSeverity getSeverity() {
        return severity;
    }

    protected void setSeverity(OpenIssueSeverity severity) {
        this.severity = severity;
    }

    @Column(name = "opened_at", nullable = false)
    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    protected void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    @Column(name = "resolved_at")
    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    protected void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Column(name = "closed_at")
    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    protected void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }
}


