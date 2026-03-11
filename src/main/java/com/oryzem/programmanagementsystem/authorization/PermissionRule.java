package com.oryzem.programmanagementsystem.authorization;

import java.util.Set;

public record PermissionRule(
        AppModule module,
        Action action,
        Set<AuthorizationRestriction> restrictions) {
}
