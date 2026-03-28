package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class OrganizationAccessService {

    private final AuthorizationService authorizationService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationDirectoryService organizationDirectoryService;

    OrganizationAccessService(
            AuthorizationService authorizationService,
            OrganizationRepository organizationRepository,
            OrganizationDirectoryService organizationDirectoryService) {
        this.authorizationService = authorizationService;
        this.organizationRepository = organizationRepository;
        this.organizationDirectoryService = organizationDirectoryService;
    }

    List<OrganizationEntity> visibleOrganizations(AuthenticatedUser actor) {
        if (canViewAllOrganizations(actor)) {
            return organizationRepository.findAllByOrderByNameAsc().stream()
                    .filter(this::isManagedOrganization)
                    .toList();
        }

        Set<String> visibleOrganizationIds = visibleOrganizationIds(actor);
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .filter(this::isManagedOrganization)
                .filter(organization -> visibleOrganizationIds.contains(organization.getId()))
                .toList();
    }

    Set<String> visibleOrganizationIds(AuthenticatedUser actor) {
        if (actor == null || actor.organizationId() == null || actor.organizationId().isBlank()) {
            return Set.of();
        }

        if (canViewAllOrganizations(actor)) {
            return organizationRepository.findAllByOrderByNameAsc().stream()
                    .filter(this::isManagedOrganization)
                    .map(OrganizationEntity::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (actor.tenantType() == TenantType.EXTERNAL && actor.hasRole(Role.SUPPORT)) {
            return Set.of(actor.organizationId());
        }

        return organizationDirectoryService.collectSubtreeIds(actor.organizationId());
    }

    OrganizationEntity findManagedOrganization(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        if (!isManagedOrganization(organization)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }
        return organization;
    }

    void assertCanViewOrganization(AuthenticatedUser actor, OrganizationEntity organization) {
        if (!visibleOrganizationIds(actor).contains(organization.getId())) {
            throw new AccessDeniedException("Organization is outside the visible hierarchy for the authenticated user.");
        }
    }

    void assertCanAccessOrganization(AuthenticatedUser actor, OrganizationEntity organization, Action action) {
        assertCanAccessOrganization(actor, organization, action, false, null);
    }

    void assertCanAccessOrganization(
            AuthenticatedUser actor,
            OrganizationEntity organization,
            Action action,
            boolean supportOverride,
            String justification) {
        AuthorizationContext context = AuthorizationContext.builder(AppModule.TENANT, action)
                .resourceTenantId(organization.getTenantId())
                .resourceTenantType(organization.getTenantType())
                .supportOverride(supportOverride)
                .justification(justification)
                .auditTrailEnabled(true)
                .build();
        assertAllowed(authorizationService.decide(actor, context));
    }

    void assertCanViewOrganizations(AuthenticatedUser actor) {
        assertAllowed(authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, Action.VIEW).build()));
    }

    void assertCanCreateRootCustomer(AuthenticatedUser actor) {
        if (actor == null || !actor.hasRole(Role.ADMIN) || actor.tenantType() != TenantType.INTERNAL) {
            throw new AccessDeniedException("Only INTERNAL admins can create root customer organizations.");
        }
    }

    void assertCanCreateChildOrganization(AuthenticatedUser actor, OrganizationEntity parentOrganization) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can create child organizations.");
        }

        if (actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.tenantType() == TenantType.EXTERNAL
                && visibleOrganizationIds(actor).contains(parentOrganization.getId())) {
            return;
        }

        throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
    }

    void assertCanManageOrganization(AuthenticatedUser actor, OrganizationEntity organization) {
        if (actor == null || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only admins can manage organizations.");
        }

        if (actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.tenantType() == TenantType.EXTERNAL
                && visibleOrganizationIds(actor).contains(organization.getId())) {
            return;
        }

        throw new AccessDeniedException("Organization is outside the manageable hierarchy for the authenticated user.");
    }

    void ensureSupportInternalPurgeActor(AuthenticatedUser actor) {
        if (actor == null || !actor.hasRole(Role.SUPPORT) || actor.tenantType() != TenantType.INTERNAL) {
            throw new AccessDeniedException("Only INTERNAL SUPPORT can purge organization subtrees.");
        }
    }

    void assertOrganizationCanOwnChildren(OrganizationEntity parentOrganization) {
        if (parentOrganization.getTenantType() != TenantType.EXTERNAL) {
            throw new IllegalArgumentException("Only EXTERNAL organizations can own child organizations.");
        }
        assertOrganizationIsActive(parentOrganization, "Parent organization");
    }

    void ensureOrganizationIsMutable(OrganizationEntity organization) {
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive organizations cannot be updated.");
        }
    }

    void assertOrganizationIsActive(OrganizationEntity organization, String label) {
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalArgumentException(label + " is inactive.");
        }
    }

    void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private boolean canViewAllOrganizations(AuthenticatedUser actor) {
        return actor != null
                && actor.tenantType() == TenantType.INTERNAL
                && (actor.hasRole(Role.ADMIN) || actor.hasRole(Role.SUPPORT));
    }

    private boolean isManagedOrganization(OrganizationEntity organization) {
        return organization.getTenantType() == TenantType.EXTERNAL;
    }
}
