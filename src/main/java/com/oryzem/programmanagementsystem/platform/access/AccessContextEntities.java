package com.oryzem.programmanagementsystem.platform.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tenant")
class TenantEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 160, nullable = false)
    private String name;

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private TenantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", length = 32, nullable = false)
    private com.oryzem.programmanagementsystem.platform.authorization.TenantType tenantType;

    @Column(name = "data_region", length = 64)
    private String dataRegion;

    @Column(name = "root_organization_id", length = 64)
    private String rootOrganizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantEntity() {
    }

    static TenantEntity create(
            String id,
            String name,
            String code,
            TenantStatus status,
            com.oryzem.programmanagementsystem.platform.authorization.TenantType tenantType,
            String dataRegion,
            String rootOrganizationId,
            Instant createdAt,
            Instant updatedAt) {
        TenantEntity tenant = new TenantEntity();
        tenant.id = id;
        tenant.name = name;
        tenant.code = code;
        tenant.status = status;
        tenant.tenantType = tenantType;
        tenant.dataRegion = dataRegion;
        tenant.rootOrganizationId = rootOrganizationId;
        tenant.createdAt = createdAt;
        tenant.updatedAt = updatedAt;
        return tenant;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getCode() {
        return code;
    }

    TenantStatus getStatus() {
        return status;
    }

    com.oryzem.programmanagementsystem.platform.authorization.TenantType getTenantType() {
        return tenantType;
    }

    String getDataRegion() {
        return dataRegion;
    }

    String getRootOrganizationId() {
        return rootOrganizationId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateFromRootOrganization(
            String name,
            String code,
            TenantStatus status,
            com.oryzem.programmanagementsystem.platform.authorization.TenantType tenantType,
            String dataRegion,
            String rootOrganizationId,
            Instant updatedAt) {
        this.name = name;
        this.code = code;
        this.status = status;
        this.tenantType = tenantType;
        this.dataRegion = dataRegion;
        this.rootOrganizationId = rootOrganizationId;
        this.updatedAt = updatedAt;
    }
}

@Entity
@Table(name = "tenant_market")
class TenantMarketEntity {

    @Id
    @Column(length = 96, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(length = 64, nullable = false)
    private String code;

    @Column(length = 160, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private MarketStatus status;

    @Column(name = "currency_code", length = 16)
    private String currencyCode;

    @Column(name = "language_code", length = 16)
    private String languageCode;

    @Column(length = 64)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantMarketEntity() {
    }

    static TenantMarketEntity create(
            String id,
            String tenantId,
            String code,
            String name,
            MarketStatus status,
            String currencyCode,
            String languageCode,
            String timezone,
            Instant createdAt,
            Instant updatedAt) {
        TenantMarketEntity market = new TenantMarketEntity();
        market.id = id;
        market.tenantId = tenantId;
        market.code = code;
        market.name = name;
        market.status = status;
        market.currencyCode = currencyCode;
        market.languageCode = languageCode;
        market.timezone = timezone;
        market.createdAt = createdAt;
        market.updatedAt = updatedAt;
        return market;
    }

    String getId() {
        return id;
    }

    String getTenantId() {
        return tenantId;
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    MarketStatus getStatus() {
        return status;
    }

    String getCurrencyCode() {
        return currencyCode;
    }

    String getLanguageCode() {
        return languageCode;
    }

    String getTimezone() {
        return timezone;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateDetails(
            String code,
            String name,
            MarketStatus status,
            String currencyCode,
            String languageCode,
            String timezone,
            Instant updatedAt) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.currencyCode = currencyCode;
        this.languageCode = languageCode;
        this.timezone = timezone;
        this.updatedAt = updatedAt;
    }

    void markInactive(Instant updatedAt) {
        this.status = MarketStatus.INACTIVE;
        this.updatedAt = updatedAt;
    }
}

@Entity
@Table(name = "user_membership")
class UserMembershipEntity {

    @Id
    @Column(length = 96, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "organization_id", length = 64)
    private String organizationId;

    @Column(name = "market_id", length = 96)
    private String marketId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private MembershipStatus status;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMembership;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserMembershipEntity() {
    }

    static UserMembershipEntity create(
            String id,
            String userId,
            String tenantId,
            String organizationId,
            String marketId,
            MembershipStatus status,
            boolean defaultMembership,
            Instant joinedAt,
            Instant updatedAt) {
        UserMembershipEntity membership = new UserMembershipEntity();
        membership.id = id;
        membership.userId = userId;
        membership.tenantId = tenantId;
        membership.organizationId = organizationId;
        membership.marketId = marketId;
        membership.status = status;
        membership.defaultMembership = defaultMembership;
        membership.joinedAt = joinedAt;
        membership.updatedAt = updatedAt;
        return membership;
    }

    static UserMembershipEntity createDefault(
            String id,
            String userId,
            String tenantId,
            String organizationId,
            String marketId,
            MembershipStatus status,
            Instant joinedAt,
            Instant updatedAt) {
        return create(id, userId, tenantId, organizationId, marketId, status, true, joinedAt, updatedAt);
    }

    String getId() {
        return id;
    }

    String getUserId() {
        return userId;
    }

    String getTenantId() {
        return tenantId;
    }

    String getOrganizationId() {
        return organizationId;
    }

    String getMarketId() {
        return marketId;
    }

    MembershipStatus getStatus() {
        return status;
    }

    boolean isDefaultMembership() {
        return defaultMembership;
    }

    Instant getJoinedAt() {
        return joinedAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateContext(
            String tenantId,
            String organizationId,
            String marketId,
            MembershipStatus status,
            boolean defaultMembership,
            Instant updatedAt) {
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.marketId = marketId;
        this.status = status;
        this.defaultMembership = defaultMembership;
        this.updatedAt = updatedAt;
    }

    void synchronizeDefaultContext(
            String tenantId,
            String organizationId,
            String marketId,
            MembershipStatus status,
            Instant updatedAt) {
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.marketId = marketId;
        this.status = status;
        this.defaultMembership = true;
        this.updatedAt = updatedAt;
    }

    void clearDefault(Instant updatedAt) {
        this.defaultMembership = false;
        this.updatedAt = updatedAt;
    }

    void markDefault(Instant updatedAt) {
        this.defaultMembership = true;
        this.updatedAt = updatedAt;
    }

    void markInactive(Instant updatedAt) {
        this.status = MembershipStatus.INACTIVE;
        this.defaultMembership = false;
        this.updatedAt = updatedAt;
    }
}

@Entity
@Table(name = "membership_role")
class MembershipRoleEntity {

    @Id
    @Column(length = 160, nullable = false)
    private String id;

    @Column(name = "membership_id", length = 96, nullable = false)
    private String membershipId;

    @Column(name = "role_code", length = 64, nullable = false)
    private String roleCode;

    protected MembershipRoleEntity() {
    }

    static MembershipRoleEntity create(String id, String membershipId, String roleCode) {
        MembershipRoleEntity entity = new MembershipRoleEntity();
        entity.id = id;
        entity.membershipId = membershipId;
        entity.roleCode = roleCode;
        return entity;
    }

    String getMembershipId() {
        return membershipId;
    }

    String getRoleCode() {
        return roleCode;
    }
}

@Entity
@Table(name = "role_permission")
class RolePermissionEntity {

    @Id
    @Column(length = 160, nullable = false)
    private String id;

    @Column(name = "role_code", length = 64, nullable = false)
    private String roleCode;

    @Column(name = "permission_code", length = 96, nullable = false)
    private String permissionCode;

    protected RolePermissionEntity() {
    }

    String getRoleCode() {
        return roleCode;
    }

    String getPermissionCode() {
        return permissionCode;
    }
}

enum TenantStatus {
    ACTIVE,
    INACTIVE
}
