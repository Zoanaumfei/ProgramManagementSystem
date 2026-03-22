package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AuthorizationMatrix authorizationMatrix;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public AuthorizationService(
            AuthorizationMatrix authorizationMatrix,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.authorizationMatrix = authorizationMatrix;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    public AuthorizationDecision decide(Authentication authentication, AuthorizationContext context) {
        return decide(authenticatedUserMapper.from(authentication), context);
    }

    public AuthorizationDecision decide(AuthenticatedUser user, AuthorizationContext context) {
        AuthorizationDecision lastDeniedDecision = null;
        for (Role role : user.roles()) {
            Optional<PermissionRule> rule = authorizationMatrix.find(role, context.module(), context.action());
            if (rule.isEmpty()) {
                continue;
            }

            AuthorizationDecision decision = applyRestrictions(user, role, rule.get(), context);
            if (decision.allowed()) {
                return decision;
            }
            lastDeniedDecision = decision;
        }

        if (lastDeniedDecision != null) {
            return lastDeniedDecision;
        }

        return AuthorizationDecision.deny("No base permission for requested operation.", Set.of(), isCrossTenant(user, context));
    }

    public boolean can(Authentication authentication, AuthorizationContext context) {
        return decide(authentication, context).allowed();
    }

    public boolean can(AuthenticatedUser user, AuthorizationContext context) {
        return decide(user, context).allowed();
    }

    public void assertCan(Authentication authentication, AuthorizationContext context) {
        AuthorizationDecision decision = decide(authentication, context);
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    public void assertCan(AuthenticatedUser user, AuthorizationContext context) {
        AuthorizationDecision decision = decide(user, context);
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private AuthorizationDecision applyRestrictions(
            AuthenticatedUser user,
            Role role,
            PermissionRule rule,
            AuthorizationContext context) {

        boolean crossTenant = isCrossTenant(user, context);
        boolean auditRequired = false;
        boolean maskedViewRequired = false;

        for (AuthorizationRestriction restriction : rule.restrictions()) {
            switch (restriction) {
                case SAME_TENANT_ONLY -> {
                    if (crossTenant) {
                        return AuthorizationDecision.deny("Operation is limited to the same tenant.", rule.restrictions(), true);
                    }
                }
                case MANAGER_TARGET_ROLE_LIMIT -> {
                    if (!role.canManageRole(context.targetRole())) {
                        return AuthorizationDecision.deny(
                                "Target role is outside the allowed management scope.",
                                rule.restrictions(),
                                crossTenant);
                    }
                }
                case MEMBER_EDIT_FLOW_RESTRICTION -> {
                    if (!isMemberEditAllowed(user, context)) {
                        return AuthorizationDecision.deny(
                                "Member edit is restricted by ownership or workflow status.",
                                rule.restrictions(),
                                crossTenant);
                    }
                }
                case AUDIT_TRAIL_REQUIRED -> {
                    auditRequired = true;
                    if (!context.auditTrailEnabled()) {
                        return AuthorizationDecision.deny(
                                "Operation requires audit trail support.",
                                rule.restrictions(),
                                crossTenant);
                    }
                }
                case JUSTIFICATION_REQUIRED -> {
                    if (!context.hasJustification()) {
                        return AuthorizationDecision.deny(
                                "Operation requires a justification.",
                                rule.restrictions(),
                                crossTenant);
                    }
                }
                case SENSITIVE_DATA_RESTRICTED -> {
                    if (context.sensitiveDataRequested() && !context.maskedViewRequested()) {
                        return AuthorizationDecision.deny(
                                "Sensitive data requires masked access.",
                                rule.restrictions(),
                                crossTenant);
                    }
                    maskedViewRequired = context.sensitiveDataRequested();
                }
                case SUPPORT_SCOPE_RESTRICTION -> {
                    if (!isSupportScopeAllowed(user, context)) {
                        return AuthorizationDecision.deny(
                                "Support action is outside the allowed support scope.",
                                rule.restrictions(),
                                crossTenant);
                    }
                }
            }
        }

        return AuthorizationDecision.allow(
                "Allowed by role %s for %s:%s.".formatted(role.name(), context.module().name(), context.action().name()),
                rule.restrictions(),
                auditRequired,
                maskedViewRequired,
                crossTenant);
    }

    private boolean isSupportScopeAllowed(AuthenticatedUser user, AuthorizationContext context) {
        if (!user.hasRole(Role.SUPPORT) && !user.hasRole(Role.ADMIN)) {
            return false;
        }

        if (!isCrossTenant(user, context)) {
            return true;
        }

        if (user.hasRole(Role.SUPPORT)
                && user.tenantType() == TenantType.INTERNAL
                && context.action() == Action.VIEW
                && (context.module() == AppModule.USERS
                        || context.module() == AppModule.TENANT
                        || context.module() == AppModule.PORTFOLIO)) {
            return true;
        }

        return context.supportOverride() && context.auditTrailEnabled() && context.hasJustification();
    }

    private boolean isMemberEditAllowed(AuthenticatedUser user, AuthorizationContext context) {
        if (context.isResourceOwnedBy(user)) {
            return true;
        }

        String resourceStatus = context.resourceStatus();
        if (resourceStatus == null || resourceStatus.isBlank()) {
            return false;
        }

        String normalizedStatus = resourceStatus.trim().toUpperCase(Locale.ROOT);
        return "DRAFT".equals(normalizedStatus) || "RETURNED".equals(normalizedStatus);
    }

    private boolean isCrossTenant(AuthenticatedUser user, AuthorizationContext context) {
        String actorTenantId = user.tenantId();
        String resourceTenantId = context.effectiveResourceTenantId(user);

        if (actorTenantId == null || actorTenantId.isBlank() || resourceTenantId == null || resourceTenantId.isBlank()) {
            return false;
        }

        return !actorTenantId.equals(resourceTenantId);
    }
}
