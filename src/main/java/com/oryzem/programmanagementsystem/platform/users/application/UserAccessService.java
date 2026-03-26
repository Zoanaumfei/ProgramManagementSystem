package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.access.ResolvedMembershipContext;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.users.api.UserSummaryResponse;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class UserAccessService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final OrganizationLookup organizationLookup;
    private final AccessContextService accessContextService;

    UserAccessService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            OrganizationLookup organizationLookup,
            AccessContextService accessContextService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.organizationLookup = organizationLookup;
        this.accessContextService = accessContextService;
    }

    ManagedUser findRequiredUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    void ensureUserIsMutable(ManagedUser user) {
        if (user.status() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive users cannot be updated.");
        }
    }

    void ensureUserCanReceiveSensitiveAction(ManagedUser user, Action action) {
        if (user.status() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive users cannot receive sensitive access actions.");
        }
        if (action == Action.RESEND_INVITE && user.status() != UserStatus.INVITED) {
            throw new IllegalArgumentException("Only invited users can receive invite resend.");
        }
        if (action == Action.RESET_ACCESS && user.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active users can receive access reset.");
        }
    }

    void ensureUserIsInactive(ManagedUser user) {
        if (user.status() != UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Only inactive users can be purged.");
        }
    }

    void ensurePurgeIsExplicitlyConfirmed(boolean supportOverride, String justification) {
        if (!supportOverride) {
            throw new IllegalArgumentException("User purge requires supportOverride=true.");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("User purge requires a justification.");
        }
    }

    void assertOrganizationChangeAllowed(ManagedUser target, String updatedOrganizationId) {
        String currentOrganizationId = resolveUserContext(target)
                .map(ResolvedMembershipContext::activeOrganizationId)
                .orElse(null);
        if (target.status() == UserStatus.ACTIVE
                && currentOrganizationId != null
                && updatedOrganizationId != null
                && !updatedOrganizationId.equals(currentOrganizationId)) {
            throw new IllegalArgumentException(
                    "Active users cannot change organization. Inactivate and recreate the user or update while still invited.");
        }
    }

    void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    void enforceOrganizationScope(
            AuthenticatedUser actor,
            String targetOrganizationId,
            String targetTenantId,
            TenantType targetTenantType,
            boolean crossTenantAllowed,
            boolean supportOverride) {
        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            if (crossTenantAllowed && supportOverride) {
                return;
            }
            if (actor.organizationId() != null && actor.organizationId().equals(targetOrganizationId)) {
                return;
            }
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() == TenantType.EXTERNAL && actor.hasRole(Role.SUPPORT)) {
            if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            Set<String> manageableOrganizationIds = organizationLookup.collectSubtreeIds(actor.organizationId());
            if (!manageableOrganizationIds.contains(targetOrganizationId)) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            return;
        }

        if (actor.organizationId() != null && !actor.organizationId().equals(targetOrganizationId)) {
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() != null && targetTenantType != null && actor.tenantType() != targetTenantType) {
            throw new AccessDeniedException("Tenant type mismatch for requested operation.");
        }
    }

    String resolveListOrganizationId(
            AuthenticatedUser actor,
            String organizationId,
            boolean supportOverride,
            String justification) {
        if (organizationId != null && !organizationId.isBlank()) {
            String requestedOrganizationId = organizationId.trim();
            enforceListScope(actor, requestedOrganizationId);
            return requestedOrganizationId;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return null;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            return null;
        }

        if (actor.hasRole(Role.SUPPORT)
                && actor.tenantType() == TenantType.INTERNAL
                && supportOverride
                && justification != null
                && !justification.isBlank()) {
            return null;
        }

        return actor.organizationId();
    }

    String resolveBoundaryTenantId(String organizationId, AuthenticatedUser actor) {
        if (organizationId == null || organizationId.isBlank()) {
            return actor.tenantId();
        }

        return organizationLookup.findById(organizationId)
                .map(OrganizationLookup.OrganizationView::tenantId)
                .orElse(actor.tenantId());
    }

    TenantType resolveListTenantType(AuthenticatedUser actor, String effectiveOrganizationId) {
        if (effectiveOrganizationId == null) {
            return null;
        }

        return organizationLookup.findById(effectiveOrganizationId)
                .map(OrganizationLookup.OrganizationView::tenantType)
                .orElse(actor.tenantType());
    }

    TenantType resolveTenantTypeForOrganization(AuthenticatedUser actor, String organizationId) {
        return organizationLookup.findById(organizationId)
                .map(OrganizationLookup.OrganizationView::tenantType)
                .or(() -> actor.organizationId() != null && actor.organizationId().equals(organizationId)
                        ? java.util.Optional.ofNullable(actor.tenantType())
                        : java.util.Optional.empty())
                .orElse(TenantType.EXTERNAL);
    }

    boolean shouldAuditView(
            AuthenticatedUser actor,
            String effectiveTenantId,
            boolean supportOverride,
            String justification) {
        if (!actor.hasRole(Role.SUPPORT)) {
            return false;
        }

        return effectiveTenantId != null
                && actor.tenantId() != null
                && !actor.tenantId().equals(effectiveTenantId)
                && supportOverride
                && justification != null
                && !justification.isBlank();
    }

    void recordAudit(
            AuthenticatedUser actor,
            String tenantId,
            String action,
            String resourceId,
            String justification,
            boolean crossTenant) {
        auditTrailService.record(new AuditTrailEvent(
                null,
                action,
                actor.subject(),
                primaryRole(actor),
                actor.tenantId(),
                tenantId,
                "USER",
                resourceId,
                justification,
                metadataJson(crossTenant),
                crossTenant,
                null,
                AppModule.USERS.name(),
                Instant.now()));
    }

    String resolveOrganizationName(String organizationId) {
        return organizationLookup.findById(organizationId)
                .map(OrganizationLookup.OrganizationView::name)
                .orElse(null);
    }

    UserSummaryResponse toSummary(ManagedUser user) {
        return UserSummaryResponse.from(user, resolveUserContext(user).isPresent());
    }

    ResolvedMembershipContext resolveRequiredUserContext(ManagedUser user) {
        return accessContextService.requireActiveContext(user);
    }

    java.util.Optional<ResolvedMembershipContext> resolveUserContext(ManagedUser user) {
        return accessContextService.resolveActiveContext(user, null);
    }

    Role resolvePrimaryRole(ManagedUser user) {
        return accessContextService.resolvePrimaryRole(user)
                .orElse(Role.MEMBER);
    }

    List<ManagedUser> selectUsersForScope(
            AuthenticatedUser actor,
            String effectiveOrganizationId,
            boolean supportOverride,
            String justification) {
        if (effectiveOrganizationId == null) {
            if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
                return userRepository.findAll();
            }

            if (actor.hasRole(Role.SUPPORT)
                    && actor.tenantType() == TenantType.INTERNAL
                    && supportOverride
                    && justification != null
                    && !justification.isBlank()) {
                return userRepository.findAll();
            }
        }

        if (effectiveOrganizationId != null) {
            return userRepository.findByTenantId(effectiveOrganizationId);
        }

        if (actor.hasRole(Role.ADMIN)
                && actor.tenantType() == TenantType.EXTERNAL
                && actor.organizationId() != null) {
            Set<String> visibleOrganizationIds = organizationLookup.collectSubtreeIds(actor.organizationId());
            return userRepository.findAll().stream()
                    .filter(user -> {
                        String organizationId = resolveRequiredUserContext(user).activeOrganizationId();
                        return organizationId != null && visibleOrganizationIds.contains(organizationId);
                    })
                    .toList();
        }

        return List.of();
    }

    void enforceListScope(AuthenticatedUser actor, String requestedTenantId) {
        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            if (actor.organizationId() != null && actor.organizationId().equals(requestedTenantId)) {
                return;
            }
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (actor.organizationId() != null
                    && organizationLookup.isSameOrDescendant(actor.organizationId(), requestedTenantId)) {
                return;
            }
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.organizationId() == null || !actor.organizationId().equals(requestedTenantId)) {
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }
    }

    void assertOrganizationCanReceiveRole(String organizationId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        if (!organizationLookup.isSetupComplete(organizationId)) {
            throw new IllegalArgumentException(
                    "Organization is incomplete and requires a first ADMIN before non-admin users can be created or updated.");
        }
    }

    String normalizeDisplayName(String displayName) {
        return displayName.trim();
    }

    String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    String normalizeOrganizationId(String organizationId) {
        return organizationId.trim();
    }

    private Role primaryRole(AuthenticatedUser actor) {
        return actor.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private String metadataJson(boolean crossTenant) {
        return "{\"crossTenant\":" + crossTenant + "}";
    }
}
