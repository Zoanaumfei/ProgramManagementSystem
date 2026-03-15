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
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final OrganizationDirectoryService organizationDirectoryService;

    public UserManagementService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            OrganizationDirectoryService organizationDirectoryService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.organizationDirectoryService = organizationDirectoryService;
    }

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
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        OrganizationDirectoryEntry organization = organizationDirectoryService.getRequired(request.organizationId().trim());
        if (!organization.active()) {
            throw new IllegalArgumentException("Inactive organization cannot receive new users.");
        }
        TenantType targetTenantType = resolveTenantTypeForOrganization(actor, organization.id());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.CREATE)
                .resourceTenantId(organization.id())
                .resourceTenantType(targetTenantType)
                .targetRole(request.role())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, organization.id(), targetTenantType);

        ManagedUser created = new ManagedUser(
                "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                request.displayName().trim(),
                request.email().trim().toLowerCase(),
                request.role(),
                organization.id(),
                targetTenantType,
                UserStatus.INVITED,
                Instant.now(),
                null,
                null);

        return UserSummaryResponse.from(userRepository.save(created), organization.name());
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

        userRepository.deleteById(userId);
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

    private UserActionResponse performSensitiveUserAction(
            AuthenticatedUser actor,
            String userId,
            Action action,
            boolean supportOverride,
            String justification) {
        ManagedUser target = findRequiredUser(userId);
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
        userRepository.save(updated);
        recordAudit(actor, target.tenantId(), action.name(), target.id(), justification, decision.crossTenant());

        return new UserActionResponse(target.id(), action.name(), performedAt, "OK");
    }

    private ManagedUser findRequiredUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
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
}
