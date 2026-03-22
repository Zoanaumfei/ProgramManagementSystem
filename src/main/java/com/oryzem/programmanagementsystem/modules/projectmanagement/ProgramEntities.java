package com.oryzem.programmanagementsystem.modules.projectmanagement;

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
@Table(name = "program_record")
class ProgramEntity extends JpaPortfolioAuditableEntity {

    private String name;
    private String code;
    private String description;
    private ProgramStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String ownerOrganizationId;
    private List<ProgramParticipationEntity> participants = new ArrayList<>();
    private List<ProjectEntity> projects = new ArrayList<>();
    private List<OpenIssueEntity> openIssues = new ArrayList<>();

    protected ProgramEntity() {
    }

    static ProgramEntity create(
            String actor,
            String name,
            String code,
            String description,
            ProgramStatus status,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            String ownerOrganizationId) {
        ProgramEntity program = new ProgramEntity();
        program.initialize(PortfolioIds.newId("PRG"), actor);
        program.name = name;
        program.code = code;
        program.description = description;
        program.status = status;
        program.plannedStartDate = plannedStartDate;
        program.plannedEndDate = plannedEndDate;
        program.ownerOrganizationId = ownerOrganizationId;
        return program;
    }

    void addParticipant(ProgramParticipationEntity participation, String actor) {
        participants.add(participation);
        touch(actor);
    }

    void addProject(ProjectEntity project, String actor) {
        projects.add(project);
        touch(actor);
    }

    void addOpenIssue(OpenIssueEntity openIssue, String actor) {
        openIssues.add(openIssue);
        touch(actor);
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(length = 80, nullable = false, unique = true)
    public String getCode() {
        return code;
    }

    protected void setCode(String code) {
        this.code = code;
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
    public ProgramStatus getStatus() {
        return status;
    }

    protected void setStatus(ProgramStatus status) {
        this.status = status;
    }

    @Column(name = "planned_start_date", nullable = false)
    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    protected void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    @Column(name = "planned_end_date", nullable = false)
    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    protected void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    @Column(name = "owner_organization_id", length = 64, nullable = false)
    public String getOwnerOrganizationId() {
        return ownerOrganizationId;
    }

    protected void setOwnerOrganizationId(String ownerOrganizationId) {
        this.ownerOrganizationId = ownerOrganizationId;
    }

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ProgramParticipationEntity> getParticipants() {
        return participants;
    }

    protected void setParticipants(List<ProgramParticipationEntity> participants) {
        this.participants = participants;
    }

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ProjectEntity> getProjects() {
        return projects;
    }

    protected void setProjects(List<ProjectEntity> projects) {
        this.projects = projects;
    }

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OpenIssueEntity> getOpenIssues() {
        return openIssues;
    }

    protected void setOpenIssues(List<OpenIssueEntity> openIssues) {
        this.openIssues = openIssues;
    }
}

@Entity
@Table(name = "program_participation")
class ProgramParticipationEntity extends JpaPortfolioAuditableEntity {

    private ProgramEntity program;
    private String organizationId;
    private ParticipationRole role;
    private ParticipationStatus status;

    protected ProgramParticipationEntity() {
    }

    static ProgramParticipationEntity create(
            String actor,
            ProgramEntity program,
            String organizationId,
            ParticipationRole role,
            ParticipationStatus status) {
        ProgramParticipationEntity participation = new ProgramParticipationEntity();
        participation.initialize(PortfolioIds.newId("PGP"), actor);
        participation.program = program;
        participation.organizationId = organizationId;
        participation.role = role;
        participation.status = status;
        return participation;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    public ProgramEntity getProgram() {
        return program;
    }

    protected void setProgram(ProgramEntity program) {
        this.program = program;
    }

    @Column(name = "organization_id", length = 64, nullable = false)
    public String getOrganizationId() {
        return organizationId;
    }

    protected void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public ParticipationRole getRole() {
        return role;
    }

    protected void setRole(ParticipationRole role) {
        this.role = role;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public ParticipationStatus getStatus() {
        return status;
    }

    protected void setStatus(ParticipationStatus status) {
        this.status = status;
    }
}

@Entity
@Table(name = "project_record")
class ProjectEntity extends JpaPortfolioAuditableEntity {

    private ProgramEntity program;
    private String name;
    private String code;
    private String description;
    private ProjectStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String appliedMilestoneTemplateId;
    private List<ProductEntity> products = new ArrayList<>();
    private List<ProjectMilestoneEntity> milestones = new ArrayList<>();

    protected ProjectEntity() {
    }

    static ProjectEntity create(
            String actor,
            ProgramEntity program,
            String name,
            String code,
            String description,
            ProjectStatus status,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            String appliedMilestoneTemplateId) {
        ProjectEntity project = new ProjectEntity();
        project.initialize(PortfolioIds.newId("PRJ"), actor);
        project.program = program;
        project.name = name;
        project.code = code;
        project.description = description;
        project.status = status;
        project.plannedStartDate = plannedStartDate;
        project.plannedEndDate = plannedEndDate;
        project.appliedMilestoneTemplateId = appliedMilestoneTemplateId;
        return project;
    }

    void addProduct(ProductEntity product, String actor) {
        products.add(product);
        touch(actor);
    }

    void addMilestone(ProjectMilestoneEntity milestone, String actor) {
        milestones.add(milestone);
        touch(actor);
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
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(length = 80, nullable = false, unique = true)
    public String getCode() {
        return code;
    }

    protected void setCode(String code) {
        this.code = code;
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
    public ProjectStatus getStatus() {
        return status;
    }

    protected void setStatus(ProjectStatus status) {
        this.status = status;
    }

    @Column(name = "planned_start_date", nullable = false)
    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    protected void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    @Column(name = "planned_end_date", nullable = false)
    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    protected void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    @Column(name = "applied_milestone_template_id", length = 64)
    public String getAppliedMilestoneTemplateId() {
        return appliedMilestoneTemplateId;
    }

    protected void setAppliedMilestoneTemplateId(String appliedMilestoneTemplateId) {
        this.appliedMilestoneTemplateId = appliedMilestoneTemplateId;
    }

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ProductEntity> getProducts() {
        return products;
    }

    protected void setProducts(List<ProductEntity> products) {
        this.products = products;
    }

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ProjectMilestoneEntity> getMilestones() {
        return milestones;
    }

    protected void setMilestones(List<ProjectMilestoneEntity> milestones) {
        this.milestones = milestones;
    }
}

@Entity
@Table(name = "product_record")
class ProductEntity extends JpaPortfolioAuditableEntity {

    private ProjectEntity project;
    private String name;
    private String code;
    private String description;
    private ProductStatus status;
    private List<ItemEntity> items = new ArrayList<>();

    protected ProductEntity() {
    }

    static ProductEntity create(
            String actor,
            ProjectEntity project,
            String name,
            String code,
            String description,
            ProductStatus status) {
        ProductEntity product = new ProductEntity();
        product.initialize(PortfolioIds.newId("PDT"), actor);
        product.project = project;
        product.name = name;
        product.code = code;
        product.description = description;
        product.status = status;
        return product;
    }

    void addItem(ItemEntity item, String actor) {
        items.add(item);
        touch(actor);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public ProjectEntity getProject() {
        return project;
    }

    protected void setProject(ProjectEntity project) {
        this.project = project;
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Column(length = 80, nullable = false, unique = true)
    public String getCode() {
        return code;
    }

    protected void setCode(String code) {
        this.code = code;
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
    public ProductStatus getStatus() {
        return status;
    }

    protected void setStatus(ProductStatus status) {
        this.status = status;
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemEntity> getItems() {
        return items;
    }

    protected void setItems(List<ItemEntity> items) {
        this.items = items;
    }
}

