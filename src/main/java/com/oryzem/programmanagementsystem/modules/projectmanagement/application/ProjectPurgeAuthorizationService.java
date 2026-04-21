package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectPurgeAuthorizationService {

    public void assertCanPurge(AuthenticatedUser actor) {
        if (actor == null
                || actor.tenantType() != TenantType.INTERNAL
                || actor.roles() == null
                || (!actor.roles().contains(Role.ADMIN) && !actor.roles().contains(Role.SUPPORT))) {
            throw new AccessDeniedException("Only internal admin or support actors can purge projects.");
        }
    }
}
