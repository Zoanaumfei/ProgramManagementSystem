package com.oryzem.programmanagementsystem.platform.authorization;

import java.util.Set;

public record PermissionRule(
        AppModule module,
        Action action,
        Set<AuthorizationRestriction> restrictions) {
}
