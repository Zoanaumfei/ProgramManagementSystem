package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.access.ResolvedMembershipContext;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.users.api.CreateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UpdateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UserSummaryResponse;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UserCommandService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final UserAccessService accessService;
    private final UserIdentityGateway userIdentityGateway;
    private final OrganizationLookup organizationLookup;
    private final AccessContextService accessContextService;

    UserCommandService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            UserAccessService accessService,
            UserIdentityGateway userIdentityGateway,
            OrganizationLookup organizationLookup,
            AccessContextService accessContextService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.userIdentityGateway = userIdentityGateway;
        this.organizationLookup = organizationLookup;
        this.accessContextService = accessContextService;
    }

    UserSummaryResponse createUser(AuthenticatedUser actor, CreateUserRequest request) {
        String normalizedEmail = accessService.normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        OrganizationLookup.OrganizationView organization = organizationLookup.getRequired(
                accessService.normalizeOrganizationId(request.organizationId()));
        if (!organization.active()) {
            throw new IllegalArgumentException("Inactive organization cannot receive new users.");
        }
        accessService.assertOrganizationCanReceiveRole(organization.id(), request.role());
        TenantType targetTenantType = accessService.resolveTenantTypeForOrganization(actor, organization.id());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.CREATE)
                .resourceTenantId(organization.tenantId())
                .resourceTenantType(targetTenantType)
                .targetRole(request.role())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(
                actor,
                organization.id(),
                organization.tenantId(),
                targetTenantType,
                decision.crossTenant(),
                false);

        String userId = "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        ManagedUser created = new ManagedUser(
                userId,
                normalizedEmail,
                null,
                accessService.normalizeDisplayName(request.displayName()),
                normalizedEmail,
                request.role(),
                organization.id(),
                targetTenantType,
                UserStatus.INVITED,
                Instant.now(),
                null,
                null);

        ManagedUser saved = userRepository.save(created);
        accessContextService.upsertDefaultMembership(
                saved.id(),
                organization.tenantId(),
                organization.id(),
                null,
                saved.status(),
                java.util.Set.of(request.role()),
                saved.createdAt());
        userIdentityGateway.createUser(saved);
        accessService.recordAudit(actor, organization.id(), "USER_CREATE", saved.id(), null, decision.crossTenant());
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

        OrganizationLookup.OrganizationView organization = organizationLookup.getRequired(
                accessService.normalizeOrganizationId(request.organizationId()));
        if (!organization.active()) {
            throw new IllegalArgumentException("Inactive organization cannot receive users.");
        }
        accessService.assertOrganizationCanReceiveRole(organization.id(), request.role());
        TenantType targetTenantType = accessService.resolveTenantTypeForOrganization(actor, organization.id());
        accessService.assertOrganizationChangeAllowed(target, organization.id());

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.EDIT)
                .resourceTenantId(organization.tenantId())
                .resourceTenantType(targetTenantType)
                .targetRole(request.role())
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(
                actor,
                organization.id(),
                organization.tenantId(),
                targetTenantType,
                decision.crossTenant(),
                false);

        ManagedUser updated = target.withUpdatedDetails(
                accessService.normalizeDisplayName(request.displayName()),
                normalizedEmail,
                request.role(),
                organization.id(),
                targetTenantType);

        ManagedUser saved = userRepository.save(updated);
        accessContextService.upsertDefaultMembership(
                saved.id(),
                organization.tenantId(),
                organization.id(),
                null,
                saved.status(),
                java.util.Set.of(request.role()),
                saved.createdAt());
        userIdentityGateway.updateUser(target, saved);
        accessService.recordAudit(actor, organization.id(), "USER_UPDATE", saved.id(), null, decision.crossTenant());
        return accessService.toSummary(saved);
    }

    void deleteUser(AuthenticatedUser actor, String userId) {
        ManagedUser target = accessService.findRequiredUser(userId);
        ResolvedMembershipContext targetContext = accessService.resolveRequiredUserContext(target);
        OrganizationLookup.OrganizationView targetOrganization =
                organizationLookup.getRequired(targetContext.activeOrganizationId());
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId(targetOrganization.tenantId())
                .resourceTenantType(targetOrganization.tenantType())
                .targetRole(accessService.resolvePrimaryRole(target))
                .targetUserId(target.id())
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(
                actor,
                targetOrganization.id(),
                targetOrganization.tenantId(),
                targetOrganization.tenantType(),
                decision.crossTenant(),
                false);

        if (target.status() == UserStatus.INACTIVE) {
            return;
        }

        ManagedUser updated = userRepository.save(target.withStatus(UserStatus.INACTIVE));
        userIdentityGateway.disableUser(updated);
        accessService.recordAudit(
                actor,
                targetOrganization.tenantId(),
                "USER_INACTIVATE",
                updated.id(),
                null,
                decision.crossTenant());
    }
}
