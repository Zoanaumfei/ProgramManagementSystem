package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OrganizationQueryService {

    private final OrganizationAccessService accessService;
    private final OrganizationSnapshotService snapshotService;

    OrganizationQueryService(
            OrganizationAccessService accessService,
            OrganizationSnapshotService snapshotService) {
        this.accessService = accessService;
        this.snapshotService = snapshotService;
    }

    List<OrganizationResponse> listOrganizations(
            AuthenticatedUser actor,
            OrganizationStatus status,
            OrganizationSetupStatus setupStatus,
            String search) {
        accessService.assertCanViewOrganizations(actor);
        List<OrganizationResponse> responses = snapshotService.toResponses(accessService.visibleOrganizations(actor));

        return responses.stream()
                .filter(organization -> status == null || organization.status() == status)
                .filter(organization -> setupStatus == null || organization.setupStatus() == setupStatus)
                .filter(organization -> matchesSearch(organization, search))
                .toList();
    }

    OrganizationResponse getOrganization(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findManagedOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.VIEW);
        accessService.assertCanViewOrganization(actor, organization);
        return snapshotService.toResponse(organization);
    }

    private boolean matchesSearch(OrganizationResponse organization, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        return organization.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || organization.code().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || organization.id().toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    // No hierarchy filters in the relationship-based model.
}
