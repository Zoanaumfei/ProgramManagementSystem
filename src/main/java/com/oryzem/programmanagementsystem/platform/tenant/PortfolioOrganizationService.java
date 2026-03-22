package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioOrganizationService {

    private final OrganizationQueryService queryService;
    private final OrganizationCommandService commandService;
    private final OrganizationPurgeService purgeService;

    PortfolioOrganizationService(
            OrganizationQueryService queryService,
            OrganizationCommandService commandService,
            OrganizationPurgeService purgeService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.purgeService = purgeService;
    }

    @Transactional(readOnly = true)
    List<OrganizationResponse> listOrganizations(
            AuthenticatedUser actor,
            OrganizationStatus status,
            OrganizationSetupStatus setupStatus,
            String customerOrganizationId,
            String parentOrganizationId,
            Integer hierarchyLevel,
            String search) {
        return queryService.listOrganizations(
                actor,
                status,
                setupStatus,
                customerOrganizationId,
                parentOrganizationId,
                hierarchyLevel,
                search);
    }

    @Transactional(readOnly = true)
    OrganizationResponse getOrganization(String organizationId, AuthenticatedUser actor) {
        return queryService.getOrganization(organizationId, actor);
    }

    OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        return commandService.createOrganization(request, actor);
    }

    OrganizationResponse updateOrganization(
            String organizationId,
            UpdateOrganizationRequest request,
            AuthenticatedUser actor) {
        return commandService.updateOrganization(organizationId, request, actor);
    }

    OrganizationResponse inactivateOrganization(String organizationId, AuthenticatedUser actor) {
        return commandService.inactivateOrganization(organizationId, actor);
    }

    OrganizationPurgeResponse purgeOrganizationSubtree(
            String organizationId,
            AuthenticatedUser actor,
            boolean supportOverride,
            String justification) {
        return purgeService.purgeOrganizationSubtree(organizationId, actor, supportOverride, justification);
    }
}
