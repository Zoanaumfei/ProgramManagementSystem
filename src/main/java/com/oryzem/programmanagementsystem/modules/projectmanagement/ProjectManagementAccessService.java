package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class ProjectManagementAccessService {

    private final AuthorizationService authorizationService;
    private final OrganizationLookup organizationLookup;
    private final ProgramRepository programRepository;

    ProjectManagementAccessService(
            AuthorizationService authorizationService,
            OrganizationLookup organizationLookup,
            ProgramRepository programRepository) {
        this.authorizationService = authorizationService;
        this.organizationLookup = organizationLookup;
        this.programRepository = programRepository;
    }

    List<ProgramEntity> visiblePrograms(AuthenticatedUser actor) {
        if (canViewAllOrganizations(actor)) {
            return programRepository.findAllByOrderByCreatedAtAsc();
        }

        Set<String> visibleOrganizationIds = visibleOrganizationIds(actor);
        return programRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(program -> visibleOrganizationIds.contains(program.getOwnerOrganizationId()))
                .toList();
    }

    OrganizationLookup.OrganizationView findOrganization(String organizationId) {
        return organizationLookup.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
    }

    OrganizationLookup.OrganizationView findPortfolioOrganization(String organizationId) {
        OrganizationLookup.OrganizationView organization = findOrganization(organizationId);
        if (!isPortfolioOrganization(organization)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }
        return organization;
    }

    void assertCanViewProgram(AuthenticatedUser actor, ProgramEntity program) {
        assertCanViewPortfolio(actor, program.getOwnerOrganizationId());
    }

    void assertCanViewPortfolio(AuthenticatedUser actor, String organizationId) {
        AuthorizationContext.Builder contextBuilder = AuthorizationContext.builder(AppModule.PORTFOLIO, Action.VIEW);
        if (organizationId != null && !organizationId.isBlank()) {
            OrganizationLookup.OrganizationView organization = findOrganization(organizationId);
            contextBuilder.resourceTenantId(organization.tenantId()).resourceTenantType(organization.tenantType());
        }
        assertAllowed(authorizationService.decide(actor, contextBuilder.build()));
        assertCanOperateOnVisiblePortfolio(actor, organizationId);
    }

    void assertCanManageProgram(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(actor, "Only ADMIN can manage programs.", Role.ADMIN);
    }

    void assertCanManageProjectLayer(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(actor, "Only ADMIN or MANAGER can manage projects, products and open issues.", Role.ADMIN, Role.MANAGER);
    }

    void assertCanManageTaskLayer(AuthenticatedUser actor, String organizationId) {
        assertCanMutatePortfolio(actor, organizationId, Action.CREATE);
        assertPortfolioRoles(
                actor,
                "Only ADMIN, MANAGER or MEMBER can manage execution items, deliverables and documents.",
                Role.ADMIN,
                Role.MANAGER,
                Role.MEMBER);
    }

    void assertCanConfigurePortfolio(AuthenticatedUser actor) {
        assertAllowed(authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.PORTFOLIO, Action.CONFIGURE).build()));
        assertPortfolioRoles(actor, "Only ADMIN can manage milestone templates.", Role.ADMIN);
    }

    void assertPortfolioOrganization(OrganizationLookup.OrganizationView organization, String label) {
        if (!isPortfolioOrganization(organization)) {
            throw new IllegalArgumentException(label + " must be EXTERNAL.");
        }
    }

    void assertOrganizationsBelongToSameCustomer(
            OrganizationLookup.OrganizationView ownerOrganization,
            OrganizationLookup.OrganizationView participantOrganization) {
        String ownerCustomerId = ownerOrganization.customerOrganizationId();
        String participantCustomerId = participantOrganization.customerOrganizationId();
        if (ownerCustomerId == null || !ownerCustomerId.equals(participantCustomerId)) {
            throw new IllegalArgumentException("Program organizations must belong to the same customer hierarchy.");
        }
    }

    void assertOrganizationIsActive(OrganizationLookup.OrganizationView organization, String label) {
        if (!organization.active()) {
            throw new IllegalArgumentException(label + " is inactive.");
        }
    }

    void assertOrganizationSetupComplete(String organizationId, String label) {
        if (!organizationLookup.isSetupComplete(organizationId)) {
            throw new IllegalArgumentException(label + " is incomplete and requires an invited or active ADMIN user.");
        }
    }

    String resolveOrganizationName(String organizationId) {
        return organizationLookup.findById(organizationId)
                .map(OrganizationLookup.OrganizationView::name)
                .orElse(null);
    }

    private Set<String> visibleOrganizationIds(AuthenticatedUser actor) {
        if (actor == null || actor.organizationId() == null || actor.organizationId().isBlank()) {
            return Set.of();
        }

        if (canViewAllOrganizations(actor)) {
            return organizationLookup.findAll().stream()
                    .filter(this::isPortfolioOrganization)
                    .map(OrganizationLookup.OrganizationView::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (actor.tenantType() == TenantType.EXTERNAL && actor.hasRole(Role.SUPPORT)) {
            return Set.of(actor.organizationId());
        }

        return organizationLookup.collectSubtreeIds(actor.organizationId());
    }

    private boolean canViewAllOrganizations(AuthenticatedUser actor) {
        return actor != null
                && actor.tenantType() == TenantType.INTERNAL
                && (actor.hasRole(Role.ADMIN) || actor.hasRole(Role.SUPPORT));
    }

    private boolean isPortfolioOrganization(OrganizationLookup.OrganizationView organization) {
        return organization.tenantType() == TenantType.EXTERNAL;
    }

    private void assertCanMutatePortfolio(AuthenticatedUser actor, String organizationId, Action action) {
        AuthorizationContext.Builder contextBuilder = AuthorizationContext.builder(AppModule.PORTFOLIO, action);
        if (organizationId != null && !organizationId.isBlank()) {
            OrganizationLookup.OrganizationView organization = findOrganization(organizationId);
            contextBuilder.resourceTenantId(organization.tenantId()).resourceTenantType(organization.tenantType());
        }
        assertAllowed(authorizationService.decide(actor, contextBuilder.build()));
        assertCanOperateOnVisiblePortfolio(actor, organizationId);
    }

    private void assertCanOperateOnVisiblePortfolio(AuthenticatedUser actor, String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return;
        }

        if (!visibleOrganizationIds(actor).contains(organizationId)) {
            throw new AccessDeniedException("Portfolio is outside the visible hierarchy for the authenticated user.");
        }
    }

    private void assertPortfolioRoles(AuthenticatedUser actor, String message, Role... allowedRoles) {
        for (Role allowedRole : allowedRoles) {
            if (actor.hasRole(allowedRole)) {
                return;
            }
        }
        throw new AccessDeniedException(message);
    }

    private void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }
}
