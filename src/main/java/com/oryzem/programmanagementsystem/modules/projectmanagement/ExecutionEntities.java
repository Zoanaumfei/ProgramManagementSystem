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


