package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationCommandService {

    private final OrganizationRepository organizationRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationAccessService accessService;
    private final OrganizationSnapshotService snapshotService;
    private final TenantUserQueryPort tenantUserQueryPort;
    private final TenantProjectPortfolioPort tenantProjectPortfolioPort;

    OrganizationCommandService(
            OrganizationRepository organizationRepository,
            AuthorizationService authorizationService,
            OrganizationAccessService accessService,
            OrganizationSnapshotService snapshotService,
            TenantUserQueryPort tenantUserQueryPort,
            TenantProjectPortfolioPort tenantProjectPortfolioPort) {
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.snapshotService = snapshotService;
        this.tenantUserQueryPort = tenantUserQueryPort;
        this.tenantProjectPortfolioPort = tenantProjectPortfolioPort;
    }

    OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        OrganizationEntity organization;
        String parentOrganizationId = trimToNull(request.parentOrganizationId());
        if (parentOrganizationId == null) {
            accessService.assertAllowed(authorizationService.decide(
                    actor,
                    AuthorizationContext.builder(AppModule.TENANT, Action.CREATE).build()));
            accessService.assertCanCreateRootCustomer(actor);
            organization = OrganizationEntity.createRootExternal(
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE));
        } else {
            OrganizationEntity parentOrganization = accessService.findPortfolioOrganization(parentOrganizationId);
            accessService.assertCanAccessOrganization(actor, parentOrganization, Action.CREATE);
            accessService.assertCanCreateChildOrganization(actor, parentOrganization);
            accessService.assertOrganizationCanOwnChildren(parentOrganization);
            organization = OrganizationEntity.createChild(
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE),
                    parentOrganization);
        }
        return snapshotService.toResponse(organizationRepository.save(organization));
    }

    OrganizationResponse updateOrganization(
            String organizationId,
            UpdateOrganizationRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findPortfolioOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.EDIT);
        accessService.assertCanManageOrganization(actor, organization);
        accessService.ensureOrganizationIsMutable(organization);

        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, organization.getId())) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        organization.updateDetails(actor.username(), normalizeName(request.name()), normalizedCode);
        return snapshotService.toResponse(organizationRepository.save(organization));
    }

    OrganizationResponse inactivateOrganization(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findPortfolioOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.DELETE);
        accessService.assertCanManageOrganization(actor, organization);
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            return snapshotService.toResponse(organization);
        }

        ensureOrganizationHasNoInvitedOrActiveUsers(organization.getId());
        ensureOrganizationHasNoActiveChildren(organization.getId());
        ensureOrganizationHasNoActiveProjects(organization.getId());
        organization.markInactive(actor.username());
        return snapshotService.toResponse(organizationRepository.save(organization));
    }

    private void ensureOrganizationHasNoInvitedOrActiveUsers(String organizationId) {
        if (tenantUserQueryPort.hasInvitedOrActiveUsers(organizationId)) {
            throw new IllegalArgumentException(
                    "Organization can only be inactivated after all invited or active users are inactivated.");
        }
    }

    private void ensureOrganizationHasNoActiveChildren(String organizationId) {
        boolean hasActiveChildren = organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).stream()
                .anyMatch(child -> child.getStatus() == OrganizationStatus.ACTIVE);
        if (hasActiveChildren) {
            throw new IllegalArgumentException("Organization can only be inactivated after all active child organizations are inactivated.");
        }
    }

    private void ensureOrganizationHasNoActiveProjects(String organizationId) {
        boolean hasActiveProjects = tenantProjectPortfolioPort.listProgramReferences().stream()
                .filter(program -> organizationId.equals(program.ownerOrganizationId()))
                .anyMatch(TenantProjectPortfolioPort.ProgramReference::hasActiveProjects);
        if (hasActiveProjects) {
            throw new IllegalArgumentException("Organization can only be inactivated after all active projects are closed or inactivated.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private <T> T defaultValue(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
