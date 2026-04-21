package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectStructureTemplateAdministrationService {

    private final OrganizationLookup organizationLookup;

    public ProjectStructureTemplateAdministrationService(OrganizationLookup organizationLookup) {
        this.organizationLookup = organizationLookup;
    }

    public void authorizeTemplateCreation(AuthenticatedUser actor) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can manage project structure templates.");
        }
        if (actor.organizationId() == null || actor.organizationId().isBlank()) {
            throw new AccessDeniedException("An active organization context is required for template administration.");
        }
    }

    public void authorizeManagement(AuthenticatedUser actor, String ownerOrganizationId) {
        authorizeTemplateCreation(actor);
        if (ownerOrganizationId == null || ownerOrganizationId.isBlank()
                || !ownerOrganizationId.equals(actor.organizationId())) {
            throw new AccessDeniedException("Only the template owner can manage this template.");
        }
    }

    public void authorizeUse(AuthenticatedUser actor, String ownerOrganizationId) {
        if (!canUse(actor, ownerOrganizationId)) {
            throw new AccessDeniedException("The authenticated organization is not allowed to use this template.");
        }
    }

    public boolean canUse(AuthenticatedUser actor, String ownerOrganizationId) {
        if (actor == null || actor.organizationId() == null || actor.organizationId().isBlank()
                || ownerOrganizationId == null || ownerOrganizationId.isBlank()) {
            return false;
        }
        return ownerOrganizationId.equals(actor.organizationId())
                || organizationLookup.isSameOrDescendant(ownerOrganizationId, actor.organizationId());
    }
}
