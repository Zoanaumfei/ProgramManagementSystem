package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.access.ResolvedMembershipContext;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.platform.users.api.UserActionResponse;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UserPurgeService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final UserAccessService accessService;
    private final UserIdentityGateway userIdentityGateway;
    private final OrganizationLookup organizationLookup;

    UserPurgeService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            UserAccessService accessService,
            UserIdentityGateway userIdentityGateway,
            OrganizationLookup organizationLookup) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.userIdentityGateway = userIdentityGateway;
        this.organizationLookup = organizationLookup;
    }

    UserActionResponse purgeUser(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        ManagedUser target = accessService.findRequiredUser(userId);
        accessService.ensureUserIsInactive(target);
        accessService.ensurePurgeIsExplicitlyConfirmed(supportOverride, justification);
        ResolvedMembershipContext targetContext = accessService.resolveRequiredManagedContext(target, Action.PURGE);
        String targetTenantId = targetContext.activeTenantId();
        String targetOrganizationId = targetContext.activeOrganizationId();
        OrganizationLookup.OrganizationView targetOrganization = targetOrganizationId == null
                ? null
                : organizationLookup.getRequired(targetOrganizationId);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.PURGE)
                .resourceTenantId(targetTenantId)
                .resourceTenantType(targetContext.tenantType())
                .targetRole(accessService.resolvePrimaryRole(target))
                .targetUserId(target.id())
                .auditTrailEnabled(true)
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        if (targetOrganization != null) {
            accessService.enforceOrganizationScope(
                    actor,
                    targetOrganization.id(),
                    targetOrganization.tenantId(),
                    targetOrganization.tenantType(),
                    decision.crossTenant(),
                    supportOverride);
        }

        if (userIdentityGateway.identityExists(target)) {
            throw new IllegalArgumentException(
                    "User purge is allowed only when the identity is already absent from Cognito.");
        }

        userRepository.deleteById(target.id());
        accessService.recordAudit(
                actor,
                targetTenantId,
                "USER_PURGE",
                target.id(),
                justification,
                decision.crossTenant());
        return new UserActionResponse(target.id(), Action.PURGE.name(), Instant.now(), "OK");
    }
}
