package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.audit.AccessAdoptionTelemetryService;
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
    private final AccessAdoptionTelemetryService telemetryService;

    UserPurgeService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            UserAccessService accessService,
            UserIdentityGateway userIdentityGateway,
            OrganizationLookup organizationLookup,
            AccessAdoptionTelemetryService telemetryService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.userIdentityGateway = userIdentityGateway;
        this.organizationLookup = organizationLookup;
        this.telemetryService = telemetryService;
    }

    UserActionResponse purgeUser(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        ManagedUser target = accessService.findRequiredUser(userId);
        OrganizationLookup.OrganizationView targetOrganization = organizationLookup.getRequired(target.tenantId());
        accessService.ensureUserIsInactive(target);
        accessService.ensurePurgeIsExplicitlyConfirmed(supportOverride, justification);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.PURGE)
                .resourceTenantId(targetOrganization.tenantId())
                .resourceTenantType(targetOrganization.tenantType())
                .targetRole(target.role())
                .targetUserId(target.id())
                .auditTrailEnabled(true)
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(
                actor,
                targetOrganization.id(),
                targetOrganization.tenantId(),
                targetOrganization.tenantType(),
                decision.crossTenant(),
                supportOverride);

        if (userIdentityGateway.identityExists(target)) {
            throw new IllegalArgumentException(
                    "User purge is allowed only when the identity is already absent from Cognito.");
        }

        userRepository.deleteById(target.id());
        accessService.recordAudit(
                actor,
                targetOrganization.tenantId(),
                "USER_PURGE",
                target.id(),
                justification,
                decision.crossTenant());
        telemetryService.recordLegacyUsersUsage(actor, "purge", targetOrganization.tenantId(), target.id());
        return new UserActionResponse(target.id(), Action.PURGE.name(), Instant.now(), "OK");
    }
}
