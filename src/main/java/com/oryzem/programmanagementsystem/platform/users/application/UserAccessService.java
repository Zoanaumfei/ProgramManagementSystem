package com.oryzem.programmanagementsystem.platform.users.application;

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

    UserAccessService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            OrganizationLookup organizationLookup) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.organizationLookup = organizationLookup;
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
        if (target.status() == UserStatus.ACTIVE
                && updatedOrganizationId != null
                && !updatedOrganizationId.equals(target.tenantId())) {
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
            if (actor.tenantId() != null && actor.tenantId().equals(targetTenantId)) {
                return;
            }
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() == TenantType.EXTERNAL && actor.hasRole(Role.SUPPORT)) {
            if (actor.tenantId() == null || !actor.tenantId().equals(targetTenantId)) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            Set<String> manageableOrganizationIds = organizationLookup.collectSubtreeIds(actor.tenantId());
            if (!manageableOrganizationIds.contains(targetTenantId)) {
                throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
            }
            return;
        }

        if (actor.tenantId() != null && !actor.tenantId().equals(targetTenantId)) {
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() != null && targetTenantType != null && actor.tenantType() != targetTenantType) {
            throw new AccessDeniedException("Tenant type mismatch for requested operation.");
        }
    }

    String resolveListTenantId(
            AuthenticatedUser actor,
            String tenantId,
            boolean supportOverride,
            String justification) {
        if (tenantId != null && !tenantId.isBlank()) {
            String requestedTenantId = tenantId.trim();
            enforceListScope(actor, requestedTenantId);
            return requestedTenantId;
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

        return actor.tenantId();
    }

    TenantType resolveListTenantType(AuthenticatedUser actor, String effectiveTenantId) {
        if (effectiveTenantId == null) {
            return null;
        }

        return organizationLookup.findById(effectiveTenantId)
                .map(OrganizationLookup.OrganizationView::tenantType)
                .orElse(actor.tenantType());
    }

    TenantType resolveTenantTypeForOrganization(AuthenticatedUser actor, String organizationId) {
        return organizationLookup.findById(organizationId)
                .map(OrganizationLookup.OrganizationView::tenantType)
                .or(() -> actor.tenantId() != null && actor.tenantId().equals(organizationId)
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

    List<ManagedUser> selectUsersForScope(
            AuthenticatedUser actor,
            String effectiveTenantId,
            boolean supportOverride,
            String justification) {
        if (effectiveTenantId == null) {
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

        if (effectiveTenantId != null) {
            return userRepository.findByTenantId(effectiveTenantId);
        }

        if (actor.hasRole(Role.ADMIN)
                && actor.tenantType() == TenantType.EXTERNAL
                && actor.tenantId() != null) {
            Set<String> visibleOrganizationIds = organizationLookup.collectSubtreeIds(actor.tenantId());
            return userRepository.findAll().stream()
                    .filter(user -> visibleOrganizationIds.contains(user.tenantId()))
                    .toList();
        }

        return List.of();
    }

    void enforceListScope(AuthenticatedUser actor, String requestedTenantId) {
        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.hasRole(Role.SUPPORT) && actor.tenantType() == TenantType.INTERNAL) {
            if (actor.tenantId() != null && actor.tenantId().equals(requestedTenantId)) {
                return;
            }
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (actor.tenantId() != null
                    && organizationLookup.isSameOrDescendant(actor.tenantId(), requestedTenantId)) {
                return;
            }
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantId() == null || !actor.tenantId().equals(requestedTenantId)) {
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
