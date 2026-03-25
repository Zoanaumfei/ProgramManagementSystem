package com.oryzem.programmanagementsystem.platform.authorization;

import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private final AccessContextService accessContextService = mock(AccessContextService.class);
    private final AuthorizationService authorizationService =
            new AuthorizationService(
                    new AuthorizationMatrix(),
                    new AuthenticatedUserMapper(accessContextService, mock(ObjectProvider.class)),
                    accessContextService);

    AuthorizationServiceTest() {
        when(accessContextService.canonicalTenantId("tenant-a")).thenReturn("TEN-tenant-a");
        when(accessContextService.canonicalTenantId("tenant-b")).thenReturn("TEN-tenant-b");
        when(accessContextService.canonicalTenantId("internal-core")).thenReturn("TEN-internal-core");
        when(accessContextService.canonicalTenantId("TEN-tenant-a")).thenReturn("TEN-tenant-a");
        when(accessContextService.canonicalTenantId("TEN-tenant-b")).thenReturn("TEN-tenant-b");
        when(accessContextService.canonicalTenantId("TEN-internal-core")).thenReturn("TEN-internal-core");
    }

    @Test
    void managerCannotDeleteMemberInUsersModule() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "manager", Set.of(Role.MANAGER), "tenant-a", TenantType.EXTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.DELETE)
                .resourceTenantId("tenant-a")
                .targetRole(Role.MEMBER)
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("No base permission");
    }

    @Test
    void internalSupportCanViewUsersCrossTenantWithoutOverride() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "support", Set.of(Role.SUPPORT), "internal-core", TenantType.INTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.USERS, Action.VIEW)
                .resourceTenantId("tenant-b")
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.crossTenant()).isTrue();
    }

    @Test
    void internalSupportCanViewTenantCrossTenantWithoutOverride() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "support", Set.of(Role.SUPPORT), "internal-core", TenantType.INTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.TENANT, Action.VIEW)
                .resourceTenantId("tenant-b")
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.crossTenant()).isTrue();
    }

    @Test
    void internalSupportCanPurgeTenantCrossTenantWithOverrideAndJustification() {
        AuthenticatedUser user = new AuthenticatedUser("user-1", "support", Set.of(Role.SUPPORT), "internal-core", TenantType.INTERNAL);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.TENANT, Action.PURGE)
                .resourceTenantId("tenant-b")
                .supportOverride(true)
                .auditTrailEnabled(true)
                .justification("Cleanup of test hierarchy")
                .build();

        AuthorizationDecision decision = authorizationService.decide(user, context);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.crossTenant()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
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

