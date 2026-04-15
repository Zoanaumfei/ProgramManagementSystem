package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddProjectOrganizationUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectOrganizationRepository organizationRepository;
    private final OrganizationLookup organizationLookup;
    private final ProjectViewMapper viewMapper;

    public AddProjectOrganizationUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectOrganizationRepository organizationRepository,
            OrganizationLookup organizationLookup,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.organizationRepository = organizationRepository;
        this.organizationLookup = organizationLookup;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectOrganizationView execute(String projectId, AddProjectOrganizationCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.ADD_ORGANIZATION);
        OrganizationLookup.OrganizationView organization = organizationLookup.findById(command.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", command.organizationId()));
        if (!organization.active() || !access.project().tenantId().equals(organization.tenantId())) {
            throw new BusinessRuleException("PROJECT_ORGANIZATION_INVALID", "Project organizations must be active and belong to the same tenant.");
        }
        ProjectOrganizationAggregate existing = organizationRepository.findByProjectIdAndOrganizationIdAndActiveTrue(projectId, command.organizationId()).orElse(null);
        if (existing != null) {
            return viewMapper.toOrganizationView(existing);
        }
        ProjectOrganizationAggregate saved = organizationRepository.save(new ProjectOrganizationAggregate(
                ProjectIds.newProjectOrganizationId(),
                projectId,
                command.organizationId(),
                command.roleType(),
                Instant.now(),
                true));
        return viewMapper.toOrganizationView(saved);
    }

    public record AddProjectOrganizationCommand(String organizationId, ProjectOrganizationRoleType roleType) {
    }
}

