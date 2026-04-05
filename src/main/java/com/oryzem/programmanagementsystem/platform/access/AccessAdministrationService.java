package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.access.api.ActivateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.ActiveAccessContextResponse;
import com.oryzem.programmanagementsystem.platform.access.api.BootstrapMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.ChangeTenantServiceTierRequest;
import com.oryzem.programmanagementsystem.platform.access.api.CreateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.CreateTenantMarketRequest;
import com.oryzem.programmanagementsystem.platform.access.api.MembershipResponse;
import com.oryzem.programmanagementsystem.platform.access.api.TenantMarketResponse;
import com.oryzem.programmanagementsystem.platform.access.api.TenantServiceTierChangeResponse;
import com.oryzem.programmanagementsystem.platform.access.api.UpdateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.UpdateTenantMarketRequest;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccessAdministrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessAdministrationService.class);

    private final UserRepository userRepository;
    private final OrganizationLookup organizationLookup;
    private final AccessContextService accessContextService;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final TenantGovernanceService tenantGovernanceService;
    private final UserIdentityGateway userIdentityGateway;
    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final SpringDataMembershipRoleJpaRepository membershipRoleRepository;
    private final SpringDataRolePermissionJpaRepository rolePermissionRepository;

    public AccessAdministrationService(
            UserRepository userRepository,
            OrganizationLookup organizationLookup,
            AccessContextService accessContextService,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            TenantGovernanceService tenantGovernanceService,
            UserIdentityGateway userIdentityGateway,
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            SpringDataMembershipRoleJpaRepository membershipRoleRepository,
            SpringDataRolePermissionJpaRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.organizationLookup = organizationLookup;
        this.accessContextService = accessContextService;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.tenantGovernanceService = tenantGovernanceService;
        this.userIdentityGateway = userIdentityGateway;
        this.tenantRepository = tenantRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMemberships(AuthenticatedUser actor, String userId) {
        findRequiredUser(userId);
        List<MembershipResponse> memberships = membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(userId).stream()
                .filter(membership -> canViewMembership(actor, membership))
                .map(this::toMembershipResponse)
                .toList();
        String targetTenantId = memberships.stream()
                .map(MembershipResponse::tenantId)
                .filter(tenantId -> tenantId != null && !tenantId.isBlank())
                .findFirst()
                .orElse(actor.tenantId());
        return memberships;
    }

    public MembershipResponse createMembership(
            AuthenticatedUser actor,
            String userId,
            CreateMembershipRequest request) {
        ManagedUser targetUser = findRequiredUser(userId);
        MembershipScope scope = resolveMembershipScope(request.tenantId(), request.organizationId(), request.marketId());
        AuthorizationDecision decision = assertMembershipMutationAllowed(actor, scope, Action.CREATE, request.roles());
        if ((request.status() == null || request.status() == MembershipStatus.ACTIVE)) {
            tenantGovernanceService.assertActiveMembershipQuotaAvailable(scope.tenant().getId());
        }

        boolean defaultMembership = request.defaultMembership()
                || membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(targetUser.id()).isEmpty();
        if (defaultMembership) {
            clearDefaultMemberships(targetUser.id(), null);
        }

        Instant now = Instant.now();
        UserMembershipEntity saved = membershipRepository.save(UserMembershipEntity.create(
                newId("MBR"),
                targetUser.id(),
                scope.tenant().getId(),
                scope.organizationId(),
                scope.marketId(),
                request.status() != null ? request.status() : MembershipStatus.ACTIVE,
                defaultMembership,
                now,
                now));
        replaceRoles(saved.getId(), request.roles());
        recordAudit(
                actor,
                scope.tenant().getId(),
                "MEMBERSHIP_CREATE",
                "MEMBERSHIP",
                saved.getId(),
                decision.crossTenant(),
                "{\"targetUserId\":\"" + saved.getUserId() + "\"}");
        return toMembershipResponse(saved);
    }

    public MembershipResponse bootstrapMembership(
            AuthenticatedUser actor,
            String userId,
            BootstrapMembershipRequest request) {
        ManagedUser targetUser = findRequiredUser(userId);
        if (!membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(targetUser.id()).isEmpty()) {
            throw new IllegalArgumentException("Bootstrap membership is allowed only for users without memberships.");
        }

        OrganizationLookup.OrganizationView organization = organizationLookup.findById(trimToNull(request.organizationId()))
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));
        MembershipScope scope = resolveMembershipScope(
                organization.tenantId(),
                organization.id(),
                request.marketId());
        AuthorizationDecision decision = assertMembershipMutationAllowed(actor, scope, Action.CREATE, request.roles());
        if (request.status() == null || request.status() == MembershipStatus.ACTIVE) {
            tenantGovernanceService.assertActiveMembershipQuotaAvailable(scope.tenant().getId());
        }

        Instant now = Instant.now();
        UserMembershipEntity saved = membershipRepository.save(UserMembershipEntity.createDefault(
                newId("MBR"),
                targetUser.id(),
                scope.tenant().getId(),
                scope.organizationId(),
                scope.marketId(),
                request.status() != null ? request.status() : MembershipStatus.ACTIVE,
                now,
                now));
        replaceRoles(saved.getId(), request.roles());
        recordAudit(
                actor,
                scope.tenant().getId(),
                "MEMBERSHIP_BOOTSTRAP",
                "MEMBERSHIP",
                saved.getId(),
                decision.crossTenant(),
                "{\"targetUserId\":\"" + saved.getUserId() + "\"}");
        return toMembershipResponse(saved);
    }

    public MembershipResponse updateMembership(
            AuthenticatedUser actor,
            String userId,
            String membershipId,
            UpdateMembershipRequest request) {
        findRequiredUser(userId);
        UserMembershipEntity existing = membershipRepository.findByIdAndUserId(membershipId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        MembershipScope scope = resolveMembershipScope(request.tenantId(), request.organizationId(), request.marketId());
        AuthorizationDecision decision = assertMembershipMutationAllowed(actor, scope, Action.EDIT, request.roles());
        if (request.status() == MembershipStatus.ACTIVE
                && (existing.getStatus() != MembershipStatus.ACTIVE || !existing.getTenantId().equals(scope.tenant().getId()))) {
            tenantGovernanceService.assertActiveMembershipQuotaAvailable(scope.tenant().getId());
        }

        if (request.defaultMembership()) {
            clearDefaultMemberships(userId, existing.getId());
        }

        existing.updateContext(
                scope.tenant().getId(),
                scope.organizationId(),
                scope.marketId(),
                request.status() != null ? request.status() : existing.getStatus(),
                request.defaultMembership(),
                Instant.now());
        UserMembershipEntity saved = membershipRepository.save(existing);
        replaceRoles(saved.getId(), request.roles());
        recordAudit(
                actor,
                scope.tenant().getId(),
                "MEMBERSHIP_UPDATE",
                "MEMBERSHIP",
                saved.getId(),
                decision.crossTenant(),
                "{\"targetUserId\":\"" + saved.getUserId() + "\"}");
        return toMembershipResponse(saved);
    }

    public MembershipResponse inactivateMembership(
            AuthenticatedUser actor,
            String userId,
            String membershipId) {
        ManagedUser targetUser = findRequiredUser(userId);
        UserMembershipEntity existing = membershipRepository.findByIdAndUserId(membershipId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        MembershipScope scope = resolveMembershipScope(existing.getTenantId(), existing.getOrganizationId(), existing.getMarketId());
        Set<Role> roles = membershipRoleRepository.findByMembershipId(existing.getId()).stream()
                .map(MembershipRoleEntity::getRoleCode)
                .map(this::toRole)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AuthorizationDecision decision = assertMembershipMutationAllowed(actor, scope, Action.DELETE, roles);

        Instant now = Instant.now();
        existing.offboard(now, tenantGovernanceService.retentionDeadlineFrom(now));
        UserMembershipEntity saved = membershipRepository.save(existing);
        if (membershipRepository.findByUserIdAndDefaultMembershipTrue(userId).isEmpty()) {
            membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(userId).stream()
                    .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                    .findFirst()
                    .ifPresent(membership -> {
                        membership.markDefault(Instant.now());
                        membershipRepository.save(membership);
                    });
        }
        boolean hasAnyActiveMembership = membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(userId).stream()
                .anyMatch(membership -> membership.getStatus() == MembershipStatus.ACTIVE);
        if (!hasAnyActiveMembership && targetUser.status() != UserStatus.INACTIVE) {
            ManagedUser disabledUser = userRepository.save(targetUser.withStatus(UserStatus.INACTIVE));
            userIdentityGateway.disableUser(disabledUser);
        }
        recordAudit(
                actor,
                scope.tenant().getId(),
                "MEMBERSHIP_OFFBOARD",
                "MEMBERSHIP",
                saved.getId(),
                decision.crossTenant(),
                "{\"targetUserId\":\"" + saved.getUserId() + "\"}");
        return toMembershipResponse(saved);
    }

    public ActiveAccessContextResponse activateMembership(
            AuthenticatedUser actor,
            ActivateMembershipRequest request) {
        if (actor.userId() == null || actor.userId().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is not linked to a local user account.");
        }

        UserMembershipEntity membership = membershipRepository.findByIdAndUserId(request.membershipId(), actor.userId())
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + request.membershipId()));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active memberships can be activated.");
        }

        if (request.makeDefault()) {
            clearDefaultMemberships(actor.userId(), membership.getId());
            membership.markDefault(Instant.now());
            membershipRepository.save(membership);
        }

        LOGGER.info(
                "Access context switch requested. actorUserId={}, previousMembershipId={}, nextMembershipId={}, makeDefault={}, tenantId={}, organizationId={}, marketId={}",
                actor.userId(),
                actor.membershipId(),
                membership.getId(),
                request.makeDefault(),
                membership.getTenantId(),
                membership.getOrganizationId(),
                membership.getMarketId());

        ManagedUser user = findRequiredUser(actor.userId());
        ResolvedMembershipContext context = accessContextService.resolveActiveContext(user, membership.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unable to resolve active access context for membership."));
        LOGGER.info(
                "Access context switch resolved. actorUserId={}, previousMembershipId={}, activeMembershipId={}, activeTenantId={}, activeOrganizationId={}, activeMarketId={}, makeDefault={}",
                actor.userId(),
                actor.membershipId(),
                context.membershipId(),
                context.activeTenantId(),
                context.activeOrganizationId(),
                context.activeMarketId(),
                request.makeDefault());
        recordAudit(
                actor,
                context.activeTenantId(),
                "ACCESS_CONTEXT_SWITCH",
                "MEMBERSHIP",
                context.membershipId(),
                !context.activeTenantId().equals(actor.activeTenantId()),
                "{\"makeDefault\":" + request.makeDefault() + "}");
        return new ActiveAccessContextResponse(
                context.userId(),
                context.membershipId(),
                context.activeTenantId(),
                context.activeOrganizationId(),
                context.activeMarketId(),
                context.tenantType(),
                context.roles().stream().map(Enum::name).sorted().toList(),
                context.permissions().stream().sorted().toList());
    }

    @Transactional(readOnly = true)
    public List<com.oryzem.programmanagementsystem.platform.access.api.TenantSummaryResponse> listVisibleTenants(
            AuthenticatedUser actor) {
        return tenantRepository.findAllByOrderByNameAsc().stream()
                .filter(tenant -> canViewTenant(actor, tenant))
                .map(tenant -> new com.oryzem.programmanagementsystem.platform.access.api.TenantSummaryResponse(
                        tenant.getId(),
                        tenant.getName(),
                        tenant.getCode(),
                        tenant.getStatus().name(),
                        tenant.getTenantType().name(),
                        tenant.getRootOrganizationId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TenantMarketResponse> listTenantMarkets(AuthenticatedUser actor, String tenantId) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        assertTenantActionAllowed(actor, tenant, Action.VIEW);
        return tenantMarketRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toTenantMarketResponse)
                .toList();
    }

    public TenantMarketResponse createTenantMarket(
            AuthenticatedUser actor,
            String tenantId,
            CreateTenantMarketRequest request) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        AuthorizationDecision decision = assertTenantActionAllowed(actor, tenant, Action.CREATE);
        tenantGovernanceService.assertMarketQuotaAvailable(tenantId);
        String normalizedCode = normalizeCode(request.code());
        if (tenantMarketRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, normalizedCode)) {
            throw new IllegalArgumentException("Market code already exists for tenant.");
        }

        Instant now = Instant.now();
        TenantMarketEntity market = TenantMarketEntity.create(
                newId("MKT"),
                tenantId,
                normalizedCode,
                normalizeName(request.name()),
                request.status() != null ? request.status() : MarketStatus.ACTIVE,
                trimToNull(request.currencyCode()),
                trimToNull(request.languageCode()),
                trimToNull(request.timezone()),
                now,
                now);
        TenantMarketResponse response = toTenantMarketResponse(tenantMarketRepository.save(market));
        recordAudit(actor, tenantId, "TENANT_MARKET_CREATE", "MARKET", response.id(), decision.crossTenant(), null);
        return response;
    }

    public TenantMarketResponse updateTenantMarket(
            AuthenticatedUser actor,
            String tenantId,
            String marketId,
            UpdateTenantMarketRequest request) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        AuthorizationDecision decision = assertTenantActionAllowed(actor, tenant, Action.EDIT);
        TenantMarketEntity market = tenantMarketRepository.findByIdAndTenantId(marketId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Market not found: " + marketId));
        String normalizedCode = normalizeCode(request.code());
        if (tenantMarketRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(tenantId, normalizedCode, marketId)) {
            throw new IllegalArgumentException("Market code already exists for tenant.");
        }

        market.updateDetails(
                normalizedCode,
                normalizeName(request.name()),
                request.status() != null ? request.status() : market.getStatus(),
                trimToNull(request.currencyCode()),
                trimToNull(request.languageCode()),
                trimToNull(request.timezone()),
                Instant.now());
        TenantMarketResponse response = toTenantMarketResponse(tenantMarketRepository.save(market));
        recordAudit(actor, tenantId, "TENANT_MARKET_UPDATE", "MARKET", response.id(), decision.crossTenant(), null);
        return response;
    }

    public TenantMarketResponse inactivateTenantMarket(
            AuthenticatedUser actor,
            String tenantId,
            String marketId) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        AuthorizationDecision decision = assertTenantActionAllowed(actor, tenant, Action.DELETE);
        TenantMarketEntity market = tenantMarketRepository.findByIdAndTenantId(marketId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Market not found: " + marketId));

        boolean marketUsedByOrganization = organizationLookup.findAll().stream()
                .anyMatch(organization -> marketId.equals(organization.marketId()));
        if (marketUsedByOrganization) {
            throw new IllegalArgumentException("Market cannot be inactivated while referenced by an organization.");
        }
        if (!membershipRepository.findAllByMarketIdAndStatus(marketId, MembershipStatus.ACTIVE).isEmpty()) {
            throw new IllegalArgumentException("Market cannot be inactivated while referenced by an active membership.");
        }

        market.markInactive(Instant.now());
        TenantMarketResponse response = toTenantMarketResponse(tenantMarketRepository.save(market));
        recordAudit(actor, tenantId, "TENANT_MARKET_INACTIVATE", "MARKET", response.id(), decision.crossTenant(), null);
        return response;
    }

    public TenantServiceTierChangeResponse changeTenantServiceTier(
            AuthenticatedUser actor,
            String tenantId,
            ChangeTenantServiceTierRequest request) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        AuthorizationDecision decision = assertTenantActionAllowed(
                actor,
                tenant,
                Action.CONFIGURE,
                actor.hasRole(Role.SUPPORT),
                request.justification());

        validateTenantServiceTierTransition(tenant, request.serviceTier());

        TenantServiceTier previousTier = tenant.getServiceTier();
        tenant.updateFromRootOrganization(
                tenant.getName(),
                tenant.getCode(),
                tenant.getStatus(),
                tenant.getTenantType(),
                tenant.getDataRegion(),
                tenant.getRootOrganizationId(),
                request.serviceTier(),
                Instant.now());
        TenantEntity saved = tenantRepository.save(tenant);
        recordAudit(
                actor,
                tenantId,
                "TENANT_SERVICE_TIER_CHANGE",
                "TENANT",
                tenantId,
                decision.crossTenant(),
                "{\"previousServiceTier\":\"" + previousTier + "\",\"serviceTier\":\""
                        + request.serviceTier() + "\",\"justification\":\"" + escapeJson(request.justification())
                        + "\"}");
        return new TenantServiceTierChangeResponse(
                saved.getId(),
                saved.getName(),
                previousTier,
                saved.getServiceTier(),
                saved.getUpdatedAt());
    }

    private ManagedUser findRequiredUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private TenantEntity findRequiredTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    private MembershipScope resolveMembershipScope(String tenantId, String organizationId, String marketId) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        String normalizedOrganizationId = trimToNull(organizationId);
        String normalizedMarketId = trimToNull(marketId);
        TenantType tenantType = tenant.getTenantType();

        if (normalizedOrganizationId != null) {
            OrganizationLookup.OrganizationView organization = organizationLookup.findById(normalizedOrganizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + normalizedOrganizationId));
            if (!tenant.getId().equals(organization.tenantId())) {
                throw new IllegalArgumentException("Organization does not belong to the provided tenant.");
            }
            tenantType = organization.tenantType();
        }

        if (normalizedMarketId != null) {
            TenantMarketEntity market = tenantMarketRepository.findByIdAndTenantId(normalizedMarketId, tenant.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Market not found: " + normalizedMarketId));
            if (market.getStatus() != MarketStatus.ACTIVE) {
                throw new IllegalArgumentException("Inactive market cannot be assigned to memberships.");
            }
        }

        return new MembershipScope(tenant, normalizedOrganizationId, normalizedMarketId, tenantType);
    }

    private void clearDefaultMemberships(String userId, String excludedMembershipId) {
        Instant now = Instant.now();
        membershipRepository.findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(userId).stream()
                .filter(UserMembershipEntity::isDefaultMembership)
                .filter(membership -> excludedMembershipId == null || !excludedMembershipId.equals(membership.getId()))
                .forEach(membership -> {
                    membership.clearDefault(now);
                    membershipRepository.save(membership);
                });
    }

    private void replaceRoles(String membershipId, Set<Role> roles) {
        membershipRoleRepository.deleteByMembershipId(membershipId);
        membershipRoleRepository.flush();
        roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .forEach(role -> membershipRoleRepository.save(MembershipRoleEntity.create(
                        newId("MRL"),
                        membershipId,
                        role.name())));
    }

    private MembershipResponse toMembershipResponse(UserMembershipEntity membership) {
        Set<Role> roles = membershipRoleRepository.findByMembershipId(membership.getId()).stream()
                .map(MembershipRoleEntity::getRoleCode)
                .map(this::toRole)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = rolePermissionRepository.findByRoleCodeIn(
                        roles.stream().map(Enum::name).toList())
                .stream()
                .map(RolePermissionEntity::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String tenantName = tenantRepository.findById(membership.getTenantId()).map(TenantEntity::getName).orElse(null);
        String organizationName = membership.getOrganizationId() == null
                ? null
                : organizationLookup.findById(membership.getOrganizationId())
                        .map(OrganizationLookup.OrganizationView::name)
                        .orElse(null);
        String marketName = membership.getMarketId() == null
                ? null
                : tenantMarketRepository.findById(membership.getMarketId()).map(TenantMarketEntity::getName).orElse(null);

        return new MembershipResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getTenantId(),
                tenantName,
                membership.getOrganizationId(),
                organizationName,
                membership.getMarketId(),
                marketName,
                membership.getStatus(),
                membership.isDefaultMembership(),
                membership.getJoinedAt(),
                membership.getUpdatedAt(),
                roles.stream().map(Enum::name).toList(),
                permissions.stream().sorted().toList());
    }

    private TenantMarketResponse toTenantMarketResponse(TenantMarketEntity market) {
        return new TenantMarketResponse(
                market.getId(),
                market.getTenantId(),
                market.getCode(),
                market.getName(),
                market.getStatus(),
                market.getCurrencyCode(),
                market.getLanguageCode(),
                market.getTimezone(),
                market.getCreatedAt(),
                market.getUpdatedAt());
    }

    private AuthorizationDecision assertMembershipMutationAllowed(
            AuthenticatedUser actor,
            MembershipScope scope,
            Action action,
            Set<Role> targetRoles) {
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.USERS, action)
                        .resourceTenantId(scope.tenant().getId())
                        .resourceTenantType(scope.tenantType())
                        .targetRole(targetRoles.stream().sorted(Comparator.comparing(Enum::name)).findFirst().orElse(null))
                        .auditTrailEnabled(true)
                        .build());
        assertAllowed(decision);

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return decision;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (!scope.tenant().getId().equals(actor.activeTenantId())) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            if (scope.organizationId() == null) {
                return decision;
            }
            if (actor.organizationId() == null
                    || !organizationLookup.isSameOrDescendant(actor.organizationId(), scope.organizationId())) {
                throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
            }
            return decision;
        }

        throw new AccessDeniedException("Membership administration is restricted to administrators.");
    }

    private boolean canViewMembership(AuthenticatedUser actor, UserMembershipEntity membership) {
        TenantType tenantType = tenantRepository.findById(membership.getTenantId())
                .map(TenantEntity::getTenantType)
                .orElse(TenantType.EXTERNAL);
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.USERS, Action.VIEW)
                        .resourceTenantId(membership.getTenantId())
                        .resourceTenantType(tenantType)
                        .auditTrailEnabled(true)
                        .build());
        if (!decision.allowed()) {
            return false;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return true;
        }
        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            return true;
        }
        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (!membership.getTenantId().equals(actor.activeTenantId())) {
                return false;
            }
            return membership.getOrganizationId() == null
                    || (actor.organizationId() != null
                            && (organizationLookup.isSameOrDescendant(actor.organizationId(), membership.getOrganizationId())
                            || organizationLookup.isDirectPartner(actor.organizationId(), membership.getOrganizationId())));
        }
        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.EXTERNAL) {
            return membership.getOrganizationId() != null
                    && actor.organizationId() != null
                    && actor.organizationId().equals(membership.getOrganizationId());
        }
        return actor.userId() != null && actor.userId().equals(membership.getUserId());
    }

    private AuthorizationDecision assertTenantActionAllowed(AuthenticatedUser actor, TenantEntity tenant, Action action) {
        return assertTenantActionAllowed(actor, tenant, action, false, null);
    }

    private AuthorizationDecision assertTenantActionAllowed(
            AuthenticatedUser actor,
            TenantEntity tenant,
            Action action,
            boolean supportOverride,
            String justification) {
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, action)
                        .resourceTenantId(tenant.getId())
                        .resourceTenantType(tenant.getTenantType())
                        .supportOverride(supportOverride)
                        .justification(justification)
                        .auditTrailEnabled(true)
                        .build());
        assertAllowed(decision);

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return decision;
        }
        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            return decision;
        }
        if (actor.activeTenantId() != null && actor.activeTenantId().equals(tenant.getId()) && actor.hasRole(Role.ADMIN)) {
            return decision;
        }

        throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
    }

    private void validateTenantServiceTierTransition(TenantEntity tenant, TenantServiceTier requestedTier) {
        if (tenant.getServiceTier() == requestedTier) {
            throw new ConflictException("Tenant is already on the requested service tier.");
        }
        if (tenant.getTenantType() == TenantType.INTERNAL && requestedTier != TenantServiceTier.INTERNAL) {
            throw new ConflictException("Internal tenants must remain on the INTERNAL service tier.");
        }
        if (tenant.getTenantType() == TenantType.EXTERNAL && requestedTier == TenantServiceTier.INTERNAL) {
            throw new ConflictException("External tenants cannot be moved to the INTERNAL service tier.");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean canViewTenant(AuthenticatedUser actor, TenantEntity tenant) {
        try {
            assertTenantActionAllowed(actor, tenant, Action.VIEW);
            return true;
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    private void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private Optional<Role> toRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(roleCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code is required.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private void recordAudit(
            AuthenticatedUser actor,
            String targetTenantId,
            String eventType,
            String targetResourceType,
            String targetResourceId,
            boolean crossTenant,
            String metadataJson) {
        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actor.userId() != null ? actor.userId() : actor.subject(),
                primaryRole(actor),
                actor.tenantId(),
                targetTenantId,
                targetResourceType,
                targetResourceId,
                null,
                metadataJson != null ? metadataJson : "{\"crossTenant\":" + crossTenant + "}",
                crossTenant,
                null,
                AppModule.TENANT.name(),
                Instant.now()));
    }

    private Role primaryRole(AuthenticatedUser actor) {
        List<Role> precedence = List.of(Role.ADMIN, Role.SUPPORT, Role.MANAGER, Role.AUDITOR, Role.MEMBER);
        return precedence.stream()
                .filter(actor.roles()::contains)
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private record MembershipScope(
            TenantEntity tenant,
            String organizationId,
            String marketId,
            TenantType tenantType) {
    }
}
