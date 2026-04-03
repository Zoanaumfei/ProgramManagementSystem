package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.access.ResolvedMembershipContext;
import com.oryzem.programmanagementsystem.platform.access.TenantGovernanceService;
import com.oryzem.programmanagementsystem.platform.users.api.CreateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UpdateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UserSummaryResponse;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UserCommandService {

    private static final Logger log = LoggerFactory.getLogger(UserCommandService.class);

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final UserAccessService accessService;
    private final UserIdentityGateway userIdentityGateway;
    private final AccessContextService accessContextService;
    private final TenantGovernanceService tenantGovernanceService;

    UserCommandService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            UserAccessService accessService,
            UserIdentityGateway userIdentityGateway,
            AccessContextService accessContextService,
            TenantGovernanceService tenantGovernanceService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.userIdentityGateway = userIdentityGateway;
        this.accessContextService = accessContextService;
        this.tenantGovernanceService = tenantGovernanceService;
    }

    UserSummaryResponse createUser(AuthenticatedUser actor, CreateUserRequest request) {
        String normalizedEmail = accessService.normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }
        UserAccessService.MembershipAssignment membershipAssignment = accessService.resolveInitialMembershipAssignment(
                actor,
                request.organizationId(),
                request.marketId(),
                request.roles());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.CREATE)
                .resourceTenantId(membershipAssignment.tenantId())
                .resourceTenantType(membershipAssignment.tenantType())
                .targetRole(request.roles().stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .findFirst()
                        .orElse(Role.MEMBER))
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(
                actor,
                membershipAssignment.organizationId(),
                membershipAssignment.tenantId(),
                membershipAssignment.tenantType(),
                decision.crossTenant(),
                false);
        tenantGovernanceService.assertActiveMembershipQuotaAvailable(membershipAssignment.tenantId());

        String userId = "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        ManagedUser created = new ManagedUser(
                userId,
                normalizedEmail,
                null,
                accessService.normalizeDisplayName(request.displayName()),
                normalizedEmail,
                UserStatus.INVITED,
                Instant.now(),
                null,
                null);

        ManagedUser saved = userRepository.save(created);
        userIdentityGateway.createUser(saved);
        try {
            accessContextService.upsertDefaultMembership(
                    saved.id(),
                    membershipAssignment.tenantId(),
                    membershipAssignment.organizationId(),
                    membershipAssignment.marketId(),
                    saved.status(),
                    request.roles(),
                    saved.createdAt());
            accessService.recordAudit(actor, membershipAssignment.tenantId(), "USER_CREATE", saved.id(), null, decision.crossTenant());
        } catch (RuntimeException exception) {
            rollbackProvisionedIdentity(saved, exception);
            throw accessService.initialMembershipProvisioningFailed(request, membershipAssignment, exception);
        }
        return accessService.toSummary(saved);
    }

    UserSummaryResponse updateUser(AuthenticatedUser actor, String userId, UpdateUserRequest request) {
        ManagedUser target = accessService.findRequiredUser(userId);
        accessService.ensureUserIsMutable(target);

        String normalizedEmail = accessService.normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.id().equals(target.id()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A user with this email already exists.");
                });

        ResolvedMembershipContext targetContext = accessService.resolveRequiredOperationalContext(target, Action.EDIT);
        String targetTenantId = targetContext.activeTenantId();
        String targetOrganizationId = targetContext.activeOrganizationId();

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.EDIT)
                .resourceTenantId(targetTenantId)
                .resourceTenantType(targetContext.tenantType())
                .targetRole(accessService.resolvePrimaryRole(target))
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        if (targetOrganizationId != null) {
            accessService.enforceOrganizationScope(
                    actor,
                    targetOrganizationId,
                    targetTenantId,
                    targetContext.tenantType(),
                    decision.crossTenant(),
                    false);
        }

        ManagedUser updated = target.withUpdatedDetails(
                accessService.normalizeDisplayName(request.displayName()),
                normalizedEmail);

        ManagedUser saved = userRepository.save(updated);
        userIdentityGateway.updateUser(target, saved);
        accessService.recordAudit(actor, targetTenantId, "USER_UPDATE", saved.id(), null, decision.crossTenant());
        return accessService.toSummary(saved);
    }

    void deleteUser(AuthenticatedUser actor, String userId) {
        ManagedUser target = accessService.findRequiredUser(userId);
        ResolvedMembershipContext targetContext = target.status() == UserStatus.INACTIVE
                ? accessService.resolveRequiredManagedContext(target, Action.DELETE)
                : accessService.resolveRequiredOperationalContext(target, Action.DELETE);
        String targetTenantId = targetContext.activeTenantId();
        String targetOrganizationId = targetContext.activeOrganizationId();
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId(targetTenantId)
                .resourceTenantType(targetContext.tenantType())
                .targetRole(accessService.resolvePrimaryRole(target))
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        if (targetOrganizationId != null) {
            accessService.enforceOrganizationScope(
                    actor,
                    targetOrganizationId,
                    targetTenantId,
                    targetContext.tenantType(),
                    decision.crossTenant(),
                    false);
        }

        if (target.status() == UserStatus.INACTIVE) {
            return;
        }

        ManagedUser updated = userRepository.save(target.withStatus(UserStatus.INACTIVE));
        userIdentityGateway.disableUser(updated);
        accessService.recordAudit(
                actor,
                targetTenantId,
                "USER_INACTIVATE",
                updated.id(),
                null,
                decision.crossTenant());
    }

    private void rollbackProvisionedIdentity(ManagedUser user, Exception cause) {
        try {
            userIdentityGateway.deleteUser(user);
        } catch (RuntimeException cleanupException) {
            log.warn(
                    "Unable to roll back provisioned identity for user {} after membership provisioning failure. cause={}, cleanupReason={}",
                    user.id(),
                    cause.getMessage(),
                    cleanupException.getMessage());
        }
    }
}
