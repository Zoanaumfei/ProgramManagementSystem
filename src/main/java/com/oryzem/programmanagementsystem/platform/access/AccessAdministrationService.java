package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.access.api.ActivateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.ActiveAccessContextResponse;
import com.oryzem.programmanagementsystem.platform.access.api.CreateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.CreateTenantMarketRequest;
import com.oryzem.programmanagementsystem.platform.access.api.MembershipResponse;
import com.oryzem.programmanagementsystem.platform.access.api.TenantMarketResponse;
import com.oryzem.programmanagementsystem.platform.access.api.UpdateMembershipRequest;
import com.oryzem.programmanagementsystem.platform.access.api.UpdateTenantMarketRequest;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.audit.AccessAdoptionTelemetryService;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
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
    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final SpringDataMembershipRoleJpaRepository membershipRoleRepository;
    private final SpringDataRolePermissionJpaRepository rolePermissionRepository;
    private final AccessAdoptionTelemetryService telemetryService;

    public AccessAdministrationService(
            UserRepository userRepository,
            OrganizationLookup organizationLookup,
            AccessContextService accessContextService,
            AuthorizationService authorizationService,
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            SpringDataMembershipRoleJpaRepository membershipRoleRepository,
            SpringDataRolePermissionJpaRepository rolePermissionRepository,
            AccessAdoptionTelemetryService telemetryService) {
        this.userRepository = userRepository;
        this.organizationLookup = organizationLookup;
        this.accessContextService = accessContextService;
        this.authorizationService = authorizationService;
        this.tenantRepository = tenantRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.telemetryService = telemetryService;
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
        telemetryService.recordMembershipUsersUsage(actor, "list", targetTenantId, userId);
        return memberships;
    }

    public MembershipResponse createMembership(
            AuthenticatedUser actor,
            String userId,
            CreateMembershipRequest request) {
        ManagedUser targetUser = findRequiredUser(userId);
        MembershipScope scope = resolveMembershipScope(request.tenantId(), request.organizationId(), request.marketId());
        assertMembershipMutationAllowed(actor, scope, Action.CREATE, request.roles());

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
        telemetryService.recordMembershipUsersUsage(actor, "create", scope.tenant().getId(), saved.getId());
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
        assertMembershipMutationAllowed(actor, scope, Action.EDIT, request.roles());

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
        telemetryService.recordMembershipUsersUsage(actor, "update", scope.tenant().getId(), saved.getId());
        return toMembershipResponse(saved);
    }

    public MembershipResponse inactivateMembership(
            AuthenticatedUser actor,
            String userId,
            String membershipId) {
        findRequiredUser(userId);
        UserMembershipEntity existing = membershipRepository.findByIdAndUserId(membershipId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        MembershipScope scope = resolveMembershipScope(existing.getTenantId(), existing.getOrganizationId(), existing.getMarketId());
        Set<Role> roles = membershipRoleRepository.findByMembershipId(existing.getId()).stream()
                .map(MembershipRoleEntity::getRoleCode)
                .map(this::toRole)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertMembershipMutationAllowed(actor, scope, Action.DELETE, roles);

        existing.markInactive(Instant.now());
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
        telemetryService.recordMembershipUsersUsage(actor, "delete", scope.tenant().getId(), saved.getId());
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
        assertTenantActionAllowed(actor, tenant, Action.CREATE);
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
        return toTenantMarketResponse(tenantMarketRepository.save(market));
    }

    public TenantMarketResponse updateTenantMarket(
            AuthenticatedUser actor,
            String tenantId,
            String marketId,
            UpdateTenantMarketRequest request) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        assertTenantActionAllowed(actor, tenant, Action.EDIT);
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
        return toTenantMarketResponse(tenantMarketRepository.save(market));
    }

    public TenantMarketResponse inactivateTenantMarket(
            AuthenticatedUser actor,
            String tenantId,
            String marketId) {
        TenantEntity tenant = findRequiredTenant(tenantId);
        assertTenantActionAllowed(actor, tenant, Action.DELETE);
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
        return toTenantMarketResponse(tenantMarketRepository.save(market));
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

    private void assertMembershipMutationAllowed(
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
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (!scope.tenant().getId().equals(actor.activeTenantId())) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            if (scope.organizationId() == null) {
                return;
            }
            if (actor.organizationId() == null
                    || !organizationLookup.isSameOrDescendant(actor.organizationId(), scope.organizationId())) {
                throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
            }
            return;
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
                            && organizationLookup.isSameOrDescendant(actor.organizationId(), membership.getOrganizationId()));
        }
        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.EXTERNAL) {
            return membership.getOrganizationId() != null
                    && actor.organizationId() != null
                    && actor.organizationId().equals(membership.getOrganizationId());
        }
        return actor.userId() != null && actor.userId().equals(membership.getUserId());
    }

    private void assertTenantActionAllowed(AuthenticatedUser actor, TenantEntity tenant, Action action) {
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, action)
                        .resourceTenantId(tenant.getId())
                        .resourceTenantType(tenant.getTenantType())
                        .auditTrailEnabled(true)
                        .build());
        assertAllowed(decision);

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }
        if (action == Action.VIEW && actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }
        if (actor.activeTenantId() != null && actor.activeTenantId().equals(tenant.getId()) && actor.hasRole(Role.ADMIN)) {
            return;
        }

        throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
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

    private record MembershipScope(
            TenantEntity tenant,
            String organizationId,
            String marketId,
            TenantType tenantType) {
    }
}
