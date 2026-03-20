package com.oryzem.programmanagementsystem.portfolio;

import com.oryzem.programmanagementsystem.authorization.TenantType;
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

@MappedSuperclass
abstract class JpaPortfolioAuditableEntity extends PortfolioAuditableEntity {

    @Id
    @Column(length = 64, nullable = false)
    @Override
    public String getId() {
        return super.getId();
    }

    @Column(name = "created_at", nullable = false)
    @Override
    public Instant getCreatedAt() {
        return super.getCreatedAt();
    }

    @Column(name = "updated_at", nullable = false)
    @Override
    public Instant getUpdatedAt() {
        return super.getUpdatedAt();
    }

    @Column(name = "created_by", nullable = false, length = 128)
    @Override
    public String getCreatedBy() {
        return super.getCreatedBy();
    }

    @Column(name = "updated_by", nullable = false, length = 128)
    @Override
    public String getUpdatedBy() {
        return super.getUpdatedBy();
    }
}

@Entity
@Table(name = "organization")
class OrganizationEntity extends JpaPortfolioAuditableEntity {

    private String name;
    private String code;
    private OrganizationStatus status;
    private TenantType tenantType;
    private OrganizationEntity parentOrganization;
    private OrganizationEntity customerOrganization;
    private Integer hierarchyLevel;

    protected OrganizationEntity() {
    }

    static OrganizationEntity createRootExternal(String actor, String name, String code, OrganizationStatus status) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(PortfolioIds.newId("ORG"), actor);
        organization.name = name;
        organization.code = code;
        organization.status = status;
        organization.tenantType = TenantType.EXTERNAL;
        organization.hierarchyLevel = 0;
        organization.customerOrganization = organization;
        return organization;
    }

    static OrganizationEntity createRootInternal(String actor, String name, String code, OrganizationStatus status) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(PortfolioIds.newId("ORG"), actor);
        organization.name = name;
        organization.code = code;
        organization.status = status;
        organization.tenantType = TenantType.INTERNAL;
        organization.hierarchyLevel = 0;
        organization.customerOrganization = null;
        return organization;
    }

    static OrganizationEntity createChild(
            String actor,
            String name,
            String code,
            OrganizationStatus status,
            OrganizationEntity parentOrganization) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(PortfolioIds.newId("ORG"), actor);
        organization.name = name;
        organization.code = code;
        organization.status = status;
        organization.tenantType = TenantType.EXTERNAL;
        organization.parentOrganization = parentOrganization;
        organization.customerOrganization = parentOrganization.getCustomerOrganization() != null
                ? parentOrganization.getCustomerOrganization()
                : parentOrganization;
        organization.hierarchyLevel = parentOrganization.getHierarchyLevel() + 1;
        return organization;
    }

    void updateDetails(String actor, String name, String code) {
        this.name = name;
        this.code = code;
        touch(actor);
    }

    void markInactive(String actor) {
        if (this.status != OrganizationStatus.INACTIVE) {
            this.status = OrganizationStatus.INACTIVE;
            touch(actor);
        }
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

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public OrganizationStatus getStatus() {
        return status;
    }

    protected void setStatus(OrganizationStatus status) {
        this.status = status;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", length = 32, nullable = false)
    public TenantType getTenantType() {
        return tenantType;
    }

    protected void setTenantType(TenantType tenantType) {
        this.tenantType = tenantType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_organization_id")
    public OrganizationEntity getParentOrganization() {
        return parentOrganization;
    }

    protected void setParentOrganization(OrganizationEntity parentOrganization) {
        this.parentOrganization = parentOrganization;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_organization_id")
    public OrganizationEntity getCustomerOrganization() {
        return customerOrganization;
    }

    protected void setCustomerOrganization(OrganizationEntity customerOrganization) {
        this.customerOrganization = customerOrganization;
    }

    @Column(name = "hierarchy_level", nullable = false)
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    protected void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }
}

@Entity
@Table(name = "program_record")
class ProgramEntity extends JpaPortfolioAuditableEntity {

    private String name;
    private String code;
    private String description;
    private ProgramStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private OrganizationEntity ownerOrganization;
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
            OrganizationEntity ownerOrganization) {
        ProgramEntity program = new ProgramEntity();
        program.initialize(PortfolioIds.newId("PRG"), actor);
        program.name = name;
        program.code = code;
        program.description = description;
        program.status = status;
        program.plannedStartDate = plannedStartDate;
        program.plannedEndDate = plannedEndDate;
        program.ownerOrganization = ownerOrganization;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_organization_id", nullable = false)
    public OrganizationEntity getOwnerOrganization() {
        return ownerOrganization;
    }

    protected void setOwnerOrganization(OrganizationEntity ownerOrganization) {
        this.ownerOrganization = ownerOrganization;
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
    private OrganizationEntity organization;
    private ParticipationRole role;
    private ParticipationStatus status;

    protected ProgramParticipationEntity() {
    }

    static ProgramParticipationEntity create(
            String actor,
            ProgramEntity program,
            OrganizationEntity organization,
            ParticipationRole role,
            ParticipationStatus status) {
        ProgramParticipationEntity participation = new ProgramParticipationEntity();
        participation.initialize(PortfolioIds.newId("PGP"), actor);
        participation.program = program;
        participation.organization = organization;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    public OrganizationEntity getOrganization() {
        return organization;
    }

    protected void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
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

@Entity
@Table(name = "item_record")
class ItemEntity extends JpaPortfolioAuditableEntity {

    private ProductEntity product;
    private String name;
    private String code;
    private String description;
    private ItemStatus status;
    private List<DeliverableEntity> deliverables = new ArrayList<>();

    protected ItemEntity() {
    }

    static ItemEntity create(
            String actor,
            ProductEntity product,
            String name,
            String code,
            String description,
            ItemStatus status) {
        ItemEntity item = new ItemEntity();
        item.initialize(PortfolioIds.newId("ITM"), actor);
        item.product = product;
        item.name = name;
        item.code = code;
        item.description = description;
        item.status = status;
        return item;
    }

    void addDeliverable(DeliverableEntity deliverable, String actor) {
        deliverables.add(deliverable);
        touch(actor);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    public ProductEntity getProduct() {
        return product;
    }

    protected void setProduct(ProductEntity product) {
        this.product = product;
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
    public ItemStatus getStatus() {
        return status;
    }

    protected void setStatus(ItemStatus status) {
        this.status = status;
    }

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<DeliverableEntity> getDeliverables() {
        return deliverables;
    }

    protected void setDeliverables(List<DeliverableEntity> deliverables) {
        this.deliverables = deliverables;
    }
}

@Entity
@Table(name = "deliverable")
class DeliverableEntity extends JpaPortfolioAuditableEntity {

    private ItemEntity item;
    private String name;
    private String description;
    private DeliverableType type;
    private DeliverableStatus status;
    private LocalDate plannedDate;
    private LocalDate dueDate;
    private OffsetDateTime submittedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime completedAt;
    private List<DeliverableDocumentEntity> documents = new ArrayList<>();

    protected DeliverableEntity() {
    }

    static DeliverableEntity create(
            String actor,
            ItemEntity item,
            String name,
            String description,
            DeliverableType type,
            DeliverableStatus status,
            LocalDate plannedDate,
            LocalDate dueDate) {
        DeliverableEntity deliverable = new DeliverableEntity();
        deliverable.initialize(PortfolioIds.newId("DLV"), actor);
        deliverable.item = item;
        deliverable.name = name;
        deliverable.description = description;
        deliverable.type = type;
        deliverable.status = status;
        deliverable.plannedDate = plannedDate;
        deliverable.dueDate = dueDate;
        return deliverable;
    }

    void addDocument(DeliverableDocumentEntity document, String actor) {
        documents.add(document);
        touch(actor);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    public ItemEntity getItem() {
        return item;
    }

    protected void setItem(ItemEntity item) {
        this.item = item;
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
    public DeliverableType getType() {
        return type;
    }

    protected void setType(DeliverableType type) {
        this.type = type;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public DeliverableStatus getStatus() {
        return status;
    }

    protected void setStatus(DeliverableStatus status) {
        this.status = status;
    }

    @Column(name = "planned_date", nullable = false)
    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    protected void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    @Column(name = "due_date", nullable = false)
    public LocalDate getDueDate() {
        return dueDate;
    }

    protected void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    @Column(name = "submitted_at")
    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    protected void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Column(name = "approved_at")
    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    protected void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    @Column(name = "completed_at")
    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    protected void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @OneToMany(mappedBy = "deliverable", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<DeliverableDocumentEntity> getDocuments() {
        return documents;
    }

    protected void setDocuments(List<DeliverableDocumentEntity> documents) {
        this.documents = documents;
    }
}

@Entity
@Table(name = "deliverable_document")
class DeliverableDocumentEntity extends JpaPortfolioAuditableEntity {

    private DeliverableEntity deliverable;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String storageBucket;
    private String storageKey;
    private DeliverableDocumentStatus status;
    private OffsetDateTime uploadedAt;

    protected DeliverableDocumentEntity() {
    }

    static DeliverableDocumentEntity createPendingUpload(
            String actor,
            DeliverableEntity deliverable,
            String fileName,
            String contentType,
            long fileSize,
            String storageBucket,
            String storageKey) {
        DeliverableDocumentEntity document = new DeliverableDocumentEntity();
        document.initialize(PortfolioIds.newId("DOC"), actor);
        document.deliverable = deliverable;
        document.fileName = fileName;
        document.contentType = contentType;
        document.fileSize = fileSize;
        document.storageBucket = storageBucket;
        document.storageKey = storageKey;
        document.status = DeliverableDocumentStatus.PENDING_UPLOAD;
        return document;
    }

    void markAvailable(String actor) {
        this.status = DeliverableDocumentStatus.AVAILABLE;
        this.uploadedAt = OffsetDateTime.now();
        touch(actor);
    }

    void markDeleted(String actor) {
        this.status = DeliverableDocumentStatus.DELETED;
        touch(actor);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deliverable_id", nullable = false)
    public DeliverableEntity getDeliverable() {
        return deliverable;
    }

    protected void setDeliverable(DeliverableEntity deliverable) {
        this.deliverable = deliverable;
    }

    @Column(name = "file_name", length = 255, nullable = false)
    public String getFileName() {
        return fileName;
    }

    protected void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Column(name = "content_type", length = 160, nullable = false)
    public String getContentType() {
        return contentType;
    }

    protected void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Column(name = "file_size", nullable = false)
    public long getFileSize() {
        return fileSize;
    }

    protected void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Column(name = "storage_bucket", length = 255, nullable = false)
    public String getStorageBucket() {
        return storageBucket;
    }

    protected void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    @Column(name = "storage_key", length = 1024, nullable = false, unique = true)
    public String getStorageKey() {
        return storageKey;
    }

    protected void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    public DeliverableDocumentStatus getStatus() {
        return status;
    }

    protected void setStatus(DeliverableDocumentStatus status) {
        this.status = status;
    }

    @Column(name = "uploaded_at")
    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    protected void setUploadedAt(OffsetDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

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
