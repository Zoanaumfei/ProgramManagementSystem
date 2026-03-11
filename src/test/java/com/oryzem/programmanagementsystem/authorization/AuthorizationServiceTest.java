package com.oryzem.programmanagementsystem.authorization;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceTest {

    private final AuthorizationService authorizationService =
            new AuthorizationService(new AuthorizationMatrix(), new AuthenticatedUserMapper());

    @Test
    void managerCanDeleteMemberInSameTenant() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "manager", Set.of(Role.MANAGER), "tenant-a", TenantType.EXTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId("tenant-a")
                .targetRole(Role.MEMBER)
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.crossTenant()).isFalse();
    }

    @Test
    void managerCannotDeleteAdmin() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "manager", Set.of(Role.MANAGER), "tenant-a", TenantType.EXTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId("tenant-a")
                .targetRole(Role.ADMIN)
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("Target role");
    }

    @Test
    void memberCanEditOwnDraftOperation() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "member", Set.of(Role.MEMBER), "tenant-a", TenantType.EXTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.EDIT)
                .resourceTenantId("tenant-a")
                .resourceOwnerUserId("user-1")
                .resourceStatus("DRAFT")
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void supportCrossTenantImpersonationRequiresOverrideAuditAndJustification() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "support", Set.of(Role.SUPPORT), "tenant-a", TenantType.INTERNAL);
        AuthorizationContext deniedContext = AuthorizationContext.builder(AppModule.SUPPORT, Action.IMPERSONATE)
                .resourceTenantId("tenant-b")
                .build();

        AuthorizationDecision denied = authorizationService.decide(user, deniedContext);

        assertThat(denied.allowed()).isFalse();

        AuthorizationContext allowedContext = AuthorizationContext.builder(AppModule.SUPPORT, Action.IMPERSONATE)
                .resourceTenantId("tenant-b")
                .supportOverride(true)
                .auditTrailEnabled(true)
                .justification("Investigating production issue")
                .build();

        AuthorizationDecision allowed = authorizationService.decide(user, allowedContext);

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.auditRequired()).isTrue();
        assertThat(allowed.crossTenant()).isTrue();
    }

    @Test
    void auditorCannotEditOperations() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "auditor", Set.of(Role.AUDITOR), "tenant-a", TenantType.EXTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.EDIT)
                .resourceTenantId("tenant-a")
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("No base permission");
    }

    @Test
    void supportSensitiveExportRequiresMaskedView() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "support", Set.of(Role.SUPPORT), "tenant-a", TenantType.INTERNAL);
        AuthorizationContext deniedContext = AuthorizationContext.builder(AppModule.REPORTS, Action.EXPORT)
                .resourceTenantId("tenant-a")
                .sensitiveDataRequested(true)
                .build();

        AuthorizationDecision denied = authorizationService.decide(user, deniedContext);

        assertThat(denied.allowed()).isFalse();

        AuthorizationContext allowedContext = AuthorizationContext.builder(AppModule.REPORTS, Action.EXPORT)
                .resourceTenantId("tenant-a")
                .sensitiveDataRequested(true)
                .maskedViewRequested(true)
                .build();

        AuthorizationDecision allowed = authorizationService.decide(user, allowedContext);

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.maskedViewRequired()).isTrue();
    }
}
