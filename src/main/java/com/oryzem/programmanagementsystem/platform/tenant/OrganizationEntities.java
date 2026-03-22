package com.oryzem.programmanagementsystem.platform.tenant;

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



