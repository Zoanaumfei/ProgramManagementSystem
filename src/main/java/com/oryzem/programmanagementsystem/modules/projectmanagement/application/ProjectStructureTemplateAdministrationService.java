package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectStructureTemplateAdministrationService {

    public void authorizeManagement(AuthenticatedUser actor) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can manage project structure templates.");
        }
    }
}
