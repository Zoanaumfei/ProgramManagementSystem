package com.oryzem.programmanagementsystem.authorization;

import java.util.Set;

public record AuthorizationDecision(
        boolean allowed,
        String reason,
        Set<AuthorizationRestriction> restrictions,
        boolean auditRequired,
        boolean maskedViewRequired,
        boolean crossTenant) {

    public static AuthorizationDecision allow(
            String reason,
            Set<AuthorizationRestriction> restrictions,
            boolean auditRequired,
            boolean maskedViewRequired,
            boolean crossTenant) {
        return new AuthorizationDecision(true, reason, restrictions, auditRequired, maskedViewRequired, crossTenant);
    }

    public static AuthorizationDecision deny(String reason, Set<AuthorizationRestriction> restrictions, boolean crossTenant) {
        return new AuthorizationDecision(false, reason, restrictions, false, false, crossTenant);
    }
}
