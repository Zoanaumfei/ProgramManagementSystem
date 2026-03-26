package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationMatrix {

    private final Map<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> rules;

    public AuthorizationMatrix() {
        EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            matrix.put(role, new EnumMap<>(AppModule.class));
        }

        grantAdmin(matrix);
        grantManager(matrix);
        grantMember(matrix);
        grantSupport(matrix);
        grantAuditor(matrix);

        this.rules = matrix;
    }

    public Optional<PermissionRule> find(Role role, AppModule module, Action action) {
        Map<AppModule, Map<Action, Set<AuthorizationRestriction>>> byModule = rules.get(role);
        if (byModule == null) {
            return Optional.empty();
        }

        Map<Action, Set<AuthorizationRestriction>> byAction = byModule.get(module);
        if (byAction == null) {
            return Optional.empty();
        }

        Set<AuthorizationRestriction> restrictions = byAction.get(action);
        if (restrictions == null) {
            return Optional.empty();
        }

        return Optional.of(new PermissionRule(module, action, restrictions));
    }

    private void grantAdmin(EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix) {
        grant(matrix, Role.ADMIN, AppModule.USERS, Action.VIEW, Action.CREATE, Action.EDIT, Action.DELETE,
                Action.RESET_ACCESS, Action.RESEND_INVITE, Action.ASSIGN_ROLE);
        grant(matrix, Role.ADMIN, AppModule.TENANT, Action.VIEW, Action.CREATE, Action.EDIT, Action.DELETE,
                Action.CONFIGURE, Action.MANAGE_INTEGRATION);
        grant(matrix, Role.ADMIN, AppModule.AUDIT, Action.VIEW, Action.EXPORT, Action.VIEW_SECURITY_EVENTS);
        grant(matrix, Role.ADMIN, AppModule.SUPPORT, Action.VIEW, Action.REPROCESS, Action.IMPERSONATE, Action.OPEN_INTERVENTION);
        grant(matrix, Role.ADMIN, AppModule.PLATFORM, Action.VIEW, Action.CONFIGURE, Action.IMPERSONATE);
    }

    private void grantManager(EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix) {
        grant(matrix, Role.MANAGER, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY), Action.VIEW, Action.VIEW_SECURITY_EVENTS);
        grant(matrix, Role.MANAGER, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY,
                AuthorizationRestriction.SENSITIVE_DATA_RESTRICTED), Action.EXPORT);

        grant(matrix, Role.MANAGER, AppModule.SUPPORT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY), Action.VIEW);
        grant(matrix, Role.MANAGER, AppModule.SUPPORT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED), Action.REPROCESS, Action.OPEN_INTERVENTION);
    }

    private void grantMember(EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix) {
        // MEMBER does not carry elevated administrative permissions in the active core.
    }

    private void grantSupport(EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix) {
        grant(matrix, Role.SUPPORT, AppModule.USERS, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION), Action.VIEW);
        grant(matrix, Role.SUPPORT, AppModule.USERS, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED), Action.RESET_ACCESS, Action.RESEND_INVITE);
        grant(matrix, Role.SUPPORT, AppModule.USERS, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED,
                AuthorizationRestriction.JUSTIFICATION_REQUIRED), Action.PURGE);

        grant(matrix, Role.SUPPORT, AppModule.TENANT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION), Action.VIEW);
        grant(matrix, Role.SUPPORT, AppModule.TENANT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED,
                AuthorizationRestriction.JUSTIFICATION_REQUIRED), Action.PURGE);

        grant(matrix, Role.SUPPORT, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION), Action.VIEW, Action.VIEW_SECURITY_EVENTS);
        grant(matrix, Role.SUPPORT, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.SENSITIVE_DATA_RESTRICTED), Action.EXPORT);

        grant(matrix, Role.SUPPORT, AppModule.SUPPORT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED), Action.REPROCESS, Action.OPEN_INTERVENTION);
        grant(matrix, Role.SUPPORT, AppModule.SUPPORT, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.AUDIT_TRAIL_REQUIRED,
                AuthorizationRestriction.JUSTIFICATION_REQUIRED), Action.IMPERSONATE);

        grant(matrix, Role.SUPPORT, AppModule.PLATFORM, restrictions(
                AuthorizationRestriction.SUPPORT_SCOPE_RESTRICTION,
                AuthorizationRestriction.SENSITIVE_DATA_RESTRICTED), Action.VIEW);
    }

    private void grantAuditor(EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix) {
        grant(matrix, Role.AUDITOR, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY), Action.VIEW, Action.VIEW_SECURITY_EVENTS);
        grant(matrix, Role.AUDITOR, AppModule.AUDIT, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY,
                AuthorizationRestriction.SENSITIVE_DATA_RESTRICTED), Action.EXPORT);
        grant(matrix, Role.AUDITOR, AppModule.PLATFORM, restrictions(
                AuthorizationRestriction.SAME_TENANT_ONLY,
                AuthorizationRestriction.SENSITIVE_DATA_RESTRICTED), Action.VIEW);
    }

    private void grant(
            EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix,
            Role role,
            AppModule module,
            Action... actions) {
        grant(matrix, role, module, restrictions(), actions);
    }

    private void grant(
            EnumMap<Role, Map<AppModule, Map<Action, Set<AuthorizationRestriction>>>> matrix,
            Role role,
            AppModule module,
            Set<AuthorizationRestriction> restrictions,
            Action... actions) {

        Map<Action, Set<AuthorizationRestriction>> byAction = matrix.get(role)
                .computeIfAbsent(module, ignored -> new EnumMap<>(Action.class));
        for (Action action : actions) {
            byAction.put(action, restrictions);
        }
    }

    private Set<AuthorizationRestriction> restrictions(AuthorizationRestriction... restrictions) {
        if (restrictions.length == 0) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.of(restrictions[0], restrictions));
    }
}
