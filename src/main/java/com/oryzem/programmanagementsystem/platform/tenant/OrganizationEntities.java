package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization")
class OrganizationEntity extends JpaAuditableEntity {

    private String name;
    private OrganizationStatus status;
    private TenantType tenantType;
    private String tenantId;
    private String marketId;
    private String cnpj;
    private OrganizationLifecycleState lifecycleState;
    private java.time.Instant offboardingStartedAt;
    private java.time.Instant offboardedAt;
    private java.time.Instant retentionUntil;
    private OrganizationDataExportStatus dataExportStatus;
    private java.time.Instant dataExportedAt;

    protected OrganizationEntity() {
    }

    static OrganizationEntity createRootExternal(
            String organizationId,
            String tenantId,
            String actor,
            String name,
            String cnpj,
            OrganizationStatus status) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(organizationId, actor);
        organization.name = name;
        organization.cnpj = cnpj;
        organization.status = status;
        organization.tenantType = TenantType.EXTERNAL;
        organization.tenantId = tenantId;
        organization.lifecycleState = OrganizationLifecycleState.ACTIVE;
        organization.dataExportStatus = OrganizationDataExportStatus.NOT_REQUESTED;
        return organization;
    }

    static OrganizationEntity createExternalForTenant(
            String organizationId,
            String tenantId,
            String marketId,
            String actor,
            String name,
            String cnpj,
            OrganizationStatus status) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(organizationId, actor);
        organization.name = name;
        organization.cnpj = cnpj;
        organization.status = status;
        organization.tenantType = TenantType.EXTERNAL;
        organization.tenantId = tenantId;
        organization.marketId = marketId;
        organization.lifecycleState = OrganizationLifecycleState.ACTIVE;
        organization.dataExportStatus = OrganizationDataExportStatus.NOT_REQUESTED;
        return organization;
    }

    static OrganizationEntity createRootInternal(
            String organizationId,
            String tenantId,
            String actor,
            String name,
            String cnpj,
            OrganizationStatus status) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.initialize(organizationId, actor);
        organization.name = name;
        organization.cnpj = cnpj;
        organization.status = status;
        organization.tenantType = TenantType.INTERNAL;
        organization.tenantId = tenantId;
        organization.lifecycleState = OrganizationLifecycleState.ACTIVE;
        organization.dataExportStatus = OrganizationDataExportStatus.NOT_REQUIRED;
        return organization;
    }

    void updateDetails(String actor, String name, String cnpj) {
        this.name = name;
        this.cnpj = cnpj;
        touch(actor);
    }

    void markInactive(String actor) {
        if (this.status != OrganizationStatus.INACTIVE) {
            this.status = OrganizationStatus.INACTIVE;
            touch(actor);
        }
    }

    void markOffboarding(String actor, java.time.Instant startedAt, java.time.Instant retentionUntil) {
        this.status = OrganizationStatus.INACTIVE;
        this.lifecycleState = OrganizationLifecycleState.OFFBOARDING;
        this.offboardingStartedAt = startedAt;
        this.retentionUntil = retentionUntil;
        if (this.tenantType == TenantType.EXTERNAL) {
            this.dataExportStatus = OrganizationDataExportStatus.READY_FOR_EXPORT;
            this.dataExportedAt = null;
        } else {
            this.dataExportStatus = OrganizationDataExportStatus.NOT_REQUIRED;
        }
        touch(actor);
    }

    void markOffboarded(String actor, java.time.Instant offboardedAt, java.time.Instant retentionUntil) {
        this.status = OrganizationStatus.INACTIVE;
        if (this.offboardingStartedAt == null) {
            this.offboardingStartedAt = offboardedAt;
        }
        this.offboardedAt = offboardedAt;
        this.retentionUntil = retentionUntil;
        this.lifecycleState = OrganizationLifecycleState.OFFBOARDED;
        if (this.tenantType == TenantType.EXTERNAL && this.dataExportStatus == OrganizationDataExportStatus.NOT_REQUESTED) {
            this.dataExportStatus = OrganizationDataExportStatus.READY_FOR_EXPORT;
        }
        touch(actor);
    }

    void markExportInProgress(String actor, java.time.Instant updatedAt) {
        if (this.lifecycleState != OrganizationLifecycleState.OFFBOARDED) {
            throw new IllegalArgumentException("Only offboarded organizations can start export.");
        }
        if (this.dataExportStatus != OrganizationDataExportStatus.READY_FOR_EXPORT) {
            throw new IllegalStateException("Organization export is not ready to start.");
        }
        this.dataExportStatus = OrganizationDataExportStatus.EXPORT_IN_PROGRESS;
        this.dataExportedAt = null;
        touch(actor);
        setUpdatedAt(updatedAt);
    }

    void markExported(String actor, java.time.Instant exportedAt) {
        if (this.lifecycleState != OrganizationLifecycleState.OFFBOARDED) {
            throw new IllegalArgumentException("Only offboarded organizations can complete export.");
        }
        if (this.dataExportStatus != OrganizationDataExportStatus.EXPORT_IN_PROGRESS) {
            throw new IllegalStateException("Organization export is not in progress.");
        }
        this.dataExportStatus = OrganizationDataExportStatus.EXPORTED;
        this.dataExportedAt = exportedAt;
        touch(actor);
        setUpdatedAt(exportedAt);
    }

    @Column(length = 160, nullable = false)
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
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

    @Column(name = "tenant_id", length = 64, nullable = false)
    public String getTenantId() {
        return tenantId;
    }

    protected void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Column(name = "market_id", length = 96)
    public String getMarketId() {
        return marketId;
    }

    protected void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    @Column(name = "cnpj", length = 14)
    public String getCnpj() {
        return cnpj;
    }

    protected void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", length = 32, nullable = false)
    public OrganizationLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    protected void setLifecycleState(OrganizationLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    @Column(name = "offboarding_started_at")
    public java.time.Instant getOffboardingStartedAt() {
        return offboardingStartedAt;
    }

    protected void setOffboardingStartedAt(java.time.Instant offboardingStartedAt) {
        this.offboardingStartedAt = offboardingStartedAt;
    }

    @Column(name = "offboarded_at")
    public java.time.Instant getOffboardedAt() {
        return offboardedAt;
    }

    protected void setOffboardedAt(java.time.Instant offboardedAt) {
        this.offboardedAt = offboardedAt;
    }

    @Column(name = "retention_until")
    public java.time.Instant getRetentionUntil() {
        return retentionUntil;
    }

    protected void setRetentionUntil(java.time.Instant retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "data_export_status", length = 32, nullable = false)
    public OrganizationDataExportStatus getDataExportStatus() {
        return dataExportStatus;
    }

    protected void setDataExportStatus(OrganizationDataExportStatus dataExportStatus) {
        this.dataExportStatus = dataExportStatus;
    }

    @Column(name = "data_exported_at")
    public java.time.Instant getDataExportedAt() {
        return dataExportedAt;
    }

    protected void setDataExportedAt(java.time.Instant dataExportedAt) {
        this.dataExportedAt = dataExportedAt;
    }
}
