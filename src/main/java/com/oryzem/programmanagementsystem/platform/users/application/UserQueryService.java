package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.api.UserSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class UserQueryService {

    private final AuthorizationService authorizationService;
    private final UserAccessService accessService;

    UserQueryService(AuthorizationService authorizationService, UserAccessService accessService) {
        this.authorizationService = authorizationService;
        this.accessService = accessService;
    }

    List<UserSummaryResponse> listUsers(
            AuthenticatedUser actor,
            String organizationId,
            boolean supportOverride,
            String justification) {
        String effectiveOrganizationId = accessService.resolveListOrganizationId(actor, organizationId, supportOverride, justification);
        String effectiveTenantId = accessService.resolveBoundaryTenantId(effectiveOrganizationId, actor);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.VIEW)
                .resourceTenantId(effectiveTenantId)
                .resourceTenantType(accessService.resolveListTenantType(actor, effectiveOrganizationId))
                .auditTrailEnabled(accessService.shouldAuditView(actor, effectiveTenantId, supportOverride, justification))
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        accessService.assertAllowed(decision);

        if (decision.auditRequired() || (decision.crossTenant() && actor.hasRole(Role.SUPPORT))) {
            accessService.recordAudit(actor, effectiveTenantId, "USERS_VIEW", null, justification, decision.crossTenant());
        }

        return accessService.selectUsersForScope(actor, effectiveOrganizationId, supportOverride, justification).stream()
                .map(accessService::toSummary)
                .toList();
    }
}
