package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.authorization.Action;
import com.oryzem.programmanagementsystem.authorization.AppModule;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.portfolio.OrganizationDirectoryService;
import com.oryzem.programmanagementsystem.portfolio.OrganizationDirectoryService.OrganizationDirectoryEntry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final UserIdentityGateway userIdentityGateway;

    public UserManagementService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            OrganizationDirectoryService organizationDirectoryService,
            UserIdentityGateway userIdentityGateway) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.userIdentityGateway = userIdentityGateway;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers(
            AuthenticatedUser actor,
            String organizationId,
            boolean supportOverride,
            String justification) {

        String effectiveTenantId = resolveListTenantId(actor, organizationId);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.VIEW)
                .resourceTenantId(effectiveTenantId)
                .resourceTenantType(resolveListTenantType(actor, effectiveTenantId))
                .auditTrailEnabled(shouldAuditView(actor, effectiveTenantId, supportOverride, justification))
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);

        if (decision.auditRequired() || (decision.crossTenant() && actor.hasRole(Role.SUPPORT))) {
            recordAudit(actor, effectiveTenantId, "USERS_VIEW", null, justification, decision.crossTenant());
        }

        List<ManagedUser> users = effectiveTenantId == null ? userRepository.findAll() : userRepository.findByTenantId(effectiveTenantId);
        return users.stream()
                .map(user -> UserSummaryResponse.from(user, resolveOrganizationName(user.tenantId())))
                .toList();
    }

    public UserSummaryResponse createUser(AuthenticatedUser actor, CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        OrganizationDirectoryEntry organization = organizationDirectoryService.getRequired(normalizeOrganizationId(request.organizationId()));
        if (!organization.active()) {
            throw new IllegalArgumentException("Inactive organization cannot receive new users.");
        }
        assertOrganizationCanReceiveRole(organization.id(), request.role());
        TenantType targetTenantType = resolveTenantTypeForOrganization(actor, organization.id());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.CREATE)
                .resourceTenantId(organization.id())
                .resourceTenantType(targetTenantType)
                .targetRole(request.role())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, organization.id(), targetTenantType);

        String userId = "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        ManagedUser created = new ManagedUser(
                userId,
                normalizedEmail,
                null,
                normalizeDisplayName(request.displayName()),
                normalizedEmail,
                request.role(),
                organization.id(),
                targetTenantType,
                UserStatus.INVITED,
                Instant.now(),
                null,
                null);

        ManagedUser saved = userRepository.save(created);
        userIdentityGateway.createUser(saved);
        recordAudit(actor, organization.id(), "USER_CREATE", saved.id(), null, decision.crossTenant());
        return UserSummaryResponse.from(saved, organization.name());
    }

    public UserSummaryResponse updateUser(AuthenticatedUser actor, String userId, UpdateUserRequest request) {
        ManagedUser target = findRequiredUser(userId);
        ensureUserIsMutable(target);

        String normalizedEmail = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.id().equals(target.id()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A user with this email already exists.");
                });

        OrganizationDirectoryEntry organization = organizationDirectoryService.getRequired(normalizeOrganizationId(request.organizationId()));
        if (!organization.active()) {
            throw new IllegalArgumentException("Inactive organization cannot receive users.");
        }
        assertOrganizationCanReceiveRole(organization.id(), request.role());
        TenantType targetTenantType = resolveTenantTypeForOrganization(actor, organization.id());
        assertOrganizationChangeAllowed(target, organization.id());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.EDIT)
                .resourceTenantId(organization.id())
                .resourceTenantType(targetTenantType)
                .targetRole(request.role())
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, organization.id(), targetTenantType);

        ManagedUser updated = target.withUpdatedDetails(
                normalizeDisplayName(request.displayName()),
                normalizedEmail,
                request.role(),
                organization.id(),
                targetTenantType);

        ManagedUser saved = userRepository.save(updated);
        userIdentityGateway.updateUser(target, saved);
        recordAudit(actor, organization.id(), "USER_UPDATE", saved.id(), null, decision.crossTenant());
        return UserSummaryResponse.from(saved, organization.name());
    }

    public void deleteUser(AuthenticatedUser actor, String userId) {
        ManagedUser target = findRequiredUser(userId);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId(target.tenantId())
                .resourceTenantType(target.tenantType())
                .targetRole(target.role())
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, target.tenantId(), target.tenantType());

        if (target.status() == UserStatus.INACTIVE) {
            return;
        }

        ManagedUser updated = userRepository.save(target.withStatus(UserStatus.INACTIVE));
        userIdentityGateway.disableUser(updated);
        recordAudit(actor, target.tenantId(), "USER_INACTIVATE", updated.id(), null, decision.crossTenant());
    }

    public UserActionResponse resendInvite(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return performSensitiveUserAction(actor, userId, Action.RESEND_INVITE, supportOverride, justification);
    }

    public UserActionResponse resetAccess(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return performSensitiveUserAction(actor, userId, Action.RESET_ACCESS, supportOverride, justification);
    }

    public UserActionResponse purgeUser(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        ManagedUser target = findRequiredUser(userId);
        ensureUserIsInactive(target);
        ensurePurgeIsExplicitlyConfirmed(supportOverride, justification);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.PURGE)
                .resourceTenantId(target.tenantId())
                .resourceTenantType(target.tenantType())
                .targetRole(target.role())
                .targetUserId(target.id())
                .auditTrailEnabled(true)
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, target.tenantId(), target.tenantType(), decision.crossTenant());

        if (userIdentityGateway.identityExists(target)) {
            throw new IllegalArgumentException(
                    "User purge is allowed only when the identity is already absent from Cognito.");
        }

        userRepository.deleteById(target.id());
        recordAudit(actor, target.tenantId(), "USER_PURGE", target.id(), justification, decision.crossTenant());
        return new UserActionResponse(target.id(), Action.PURGE.name(), Instant.now(), "OK");
    }

    private UserActionResponse performSensitiveUserAction(
            AuthenticatedUser actor,
            String userId,
            Action action,
            boolean supportOverride,
            String justification) {
        ManagedUser target = findRequiredUser(userId);
        ensureUserCanReceiveSensitiveAction(target, action);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, action)
                .resourceTenantId(target.tenantId())
                .resourceTenantType(target.tenantType())
                .targetRole(target.role())
                .targetUserId(target.id())
                .auditTrailEnabled(true)
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, target.tenantId(), target.tenantType(), decision.crossTenant());

        Instant performedAt = Instant.now();
        ManagedUser updated = switch (action) {
            case RESEND_INVITE -> target.withInviteResentAt(performedAt);
            case RESET_ACCESS -> target.withAccessResetAt(performedAt);
            default -> throw new IllegalArgumentException("Unsupported user action: " + action);
        };
        ManagedUser saved = userRepository.save(updated);
        if (action == Action.RESEND_INVITE) {
            userIdentityGateway.resendInvite(saved);
        } else {
            userIdentityGateway.resetAccess(saved);
        }
        recordAudit(actor, target.tenantId(), action.name(), target.id(), justification, decision.crossTenant());

        return new UserActionResponse(target.id(), action.name(), performedAt, "OK");
    }

    public void synchronizeAuthenticatedUser(String identitySubject, String identityUsername, String email) {
        boolean hasSubject = identitySubject != null && !identitySubject.isBlank();
        boolean hasUsername = identityUsername != null && !identityUsername.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (!hasSubject && !hasUsername && !hasEmail) {
            return;
        }

        ManagedUser user = null;
        if (hasSubject) {
            user = userRepository.findByIdentitySubject(identitySubject).orElse(null);
        }
        if (user == null && hasUsername) {
            user = userRepository.findByIdentityUsername(identityUsername).orElse(null);
        }
        if (user == null && hasEmail) {
            user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (user == null) {
            return;
        }

        ManagedUser updated = user;
        if (hasSubject
                && (user.identitySubject() == null || !identitySubject.equals(user.identitySubject()))) {
            updated = updated.withIdentitySubject(identitySubject);
        }
        if (updated.status() == UserStatus.INVITED) {
            updated = updated.withStatus(UserStatus.ACTIVE);
        }

        if (!updated.equals(user)) {
            userRepository.save(updated);
        }
    }

    private ManagedUser findRequiredUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void ensureUserIsMutable(ManagedUser user) {
        if (user.status() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive users cannot be updated.");
        }
    }

    private void ensureUserIsActive(ManagedUser user) {
        if (user.status() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive users cannot receive sensitive access actions.");
        }
    }

    private void ensureUserCanReceiveSensitiveAction(ManagedUser user, Action action) {
        ensureUserIsActive(user);
        if (action == Action.RESEND_INVITE && user.status() != UserStatus.INVITED) {
            throw new IllegalArgumentException("Only invited users can receive invite resend.");
        }
        if (action == Action.RESET_ACCESS && user.status() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active users can receive access reset.");
        }
    }

    private void ensureUserIsInactive(ManagedUser user) {
        if (user.status() != UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Only inactive users can be purged.");
        }
    }

    private void ensurePurgeIsExplicitlyConfirmed(boolean supportOverride, String justification) {
        if (!supportOverride) {
            throw new IllegalArgumentException("User purge requires supportOverride=true.");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("User purge requires a justification.");
        }
    }

    private void assertOrganizationChangeAllowed(ManagedUser target, String updatedOrganizationId) {
        if (target.status() == UserStatus.ACTIVE
                && updatedOrganizationId != null
                && !updatedOrganizationId.equals(target.tenantId())) {
            throw new IllegalArgumentException(
                    "Active users cannot change organization. Inactivate and recreate the user or update while still invited.");
        }
    }

    private void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private void enforceTenantScope(AuthenticatedUser actor, String targetTenantId, TenantType targetTenantType) {
        enforceTenantScope(actor, targetTenantId, targetTenantType, false);
    }

    private void enforceTenantScope(
            AuthenticatedUser actor,
            String targetTenantId,
            TenantType targetTenantType,
            boolean crossTenantAllowed) {
        if (actor.isAdmin() || crossTenantAllowed) {
            return;
        }

        if (actor.tenantId() != null && !actor.tenantId().equals(targetTenantId)) {
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() != null && targetTenantType != null && actor.tenantType() != targetTenantType) {
            throw new AccessDeniedException("Tenant type mismatch for requested operation.");
        }
    }

    private String resolveListTenantId(AuthenticatedUser actor, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.trim();
        }

        return actor.isAdmin() ? null : actor.tenantId();
    }

    private TenantType resolveListTenantType(AuthenticatedUser actor, String effectiveTenantId) {
        if (effectiveTenantId == null) {
            return null;
        }

        return userRepository.findByTenantId(effectiveTenantId).stream()
                .map(ManagedUser::tenantType)
                .findFirst()
                .orElse(actor.tenantType());
    }

    private TenantType resolveTenantTypeForOrganization(AuthenticatedUser actor, String organizationId) {
        return userRepository.findByTenantId(organizationId).stream()
                .map(ManagedUser::tenantType)
                .findFirst()
                .or(() -> actor.tenantId() != null && actor.tenantId().equals(organizationId)
                        ? java.util.Optional.ofNullable(actor.tenantType())
                        : java.util.Optional.empty())
                .orElse(TenantType.EXTERNAL);
    }

    private boolean shouldAuditView(
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

    private void recordAudit(
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

    private Role primaryRole(AuthenticatedUser actor) {
        return actor.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private String metadataJson(boolean crossTenant) {
        return "{\"crossTenant\":" + crossTenant + "}";
    }

    private String resolveOrganizationName(String organizationId) {
        return organizationDirectoryService.findById(organizationId)
                .map(OrganizationDirectoryEntry::name)
                .orElse(null);
    }

    private void assertOrganizationCanReceiveRole(String organizationId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        if (!userRepository.hasInvitedOrActiveAdmin(organizationId)) {
            throw new IllegalArgumentException(
                    "Organization is incomplete and requires a first ADMIN before non-admin users can be created or updated.");
        }
    }

    private String normalizeDisplayName(String displayName) {
        return displayName.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOrganizationId(String organizationId) {
        return organizationId.trim();
    }
}
