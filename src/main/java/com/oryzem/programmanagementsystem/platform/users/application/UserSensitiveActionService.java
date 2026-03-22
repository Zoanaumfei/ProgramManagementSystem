package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.users.api.UserActionResponse;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UserSensitiveActionService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final UserAccessService accessService;
    private final UserIdentityGateway userIdentityGateway;

    UserSensitiveActionService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            UserAccessService accessService,
            UserIdentityGateway userIdentityGateway) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.userIdentityGateway = userIdentityGateway;
    }

    UserActionResponse resendInvite(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return performSensitiveUserAction(actor, userId, Action.RESEND_INVITE, supportOverride, justification);
    }

    UserActionResponse resetAccess(
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
        ManagedUser target = accessService.findRequiredUser(userId);
        accessService.ensureUserCanReceiveSensitiveAction(target, action);
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
        accessService.assertAllowed(decision);
        accessService.enforceOrganizationScope(actor, target.tenantId(), target.tenantType(), decision.crossTenant(), supportOverride);

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
        accessService.recordAudit(actor, target.tenantId(), action.name(), target.id(), justification, decision.crossTenant());

        return new UserActionResponse(target.id(), action.name(), performedAt, "OK");
    }
}
