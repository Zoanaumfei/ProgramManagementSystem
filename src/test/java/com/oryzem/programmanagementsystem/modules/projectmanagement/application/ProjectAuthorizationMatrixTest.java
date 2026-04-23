package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectEntity;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectAuthorizationMatrixTest {

    private ProjectAuthorizationService authorizationService;

    private final ProjectAggregate project = new ProjectAggregate(
            "PRJ-1",
            "tenant-a",
            "PRJ-001",
            "Project",
            "desc",
            "APQP",
            "TMP-1",
            1,
            "org-lead",
            "org-customer",
            ProjectStatus.PLANNED,
            ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            null,
            null,
            "USR-CREATOR",
            Instant.now(),
            Instant.now(),
            0L);

    private final List<ProjectOrganizationAggregate> organizations = List.of(
            new ProjectOrganizationAggregate("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now(), true),
            new ProjectOrganizationAggregate("PO-CUSTOMER", "PRJ-1", "org-customer", ProjectOrganizationRoleType.CUSTOMER, Instant.now(), true),
            new ProjectOrganizationAggregate("PO-SUPPLIER", "PRJ-1", "org-supplier", ProjectOrganizationRoleType.SUPPLIER, Instant.now(), true),
            new ProjectOrganizationAggregate("PO-PARTNER", "PRJ-1", "org-partner", ProjectOrganizationRoleType.PARTNER, Instant.now(), true));

    @BeforeEach
    void setUp() {
        AccessContextService accessContextService = mock(AccessContextService.class);
        when(accessContextService.canonicalTenantId("tenant-a")).thenReturn("tenant-a");
        ProjectManagementProperties properties = new ProjectManagementProperties();
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        ProjectAccessPolicy projectAccessPolicy = new ProjectAccessPolicy(visibilityPolicy);
        ProjectDeliverableAccessPolicy deliverableAccessPolicy = new ProjectDeliverableAccessPolicy(visibilityPolicy);
        authorizationService = new ProjectAuthorizationService(
                properties,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                accessContextService,
                mock(ProjectAuditService.class),
                projectAccessPolicy,
                new ProjectMilestoneAccessPolicy(projectAccessPolicy, visibilityPolicy),
                deliverableAccessPolicy,
                new DeliverableSubmissionAccessPolicy(deliverableAccessPolicy),
                new ProjectStructureNodeAccessPolicy(projectAccessPolicy, visibilityPolicy));
    }

    @Test
    void shouldAllowViewingProjectForEveryActiveOrganizationRole() {
        assertThat(authorizationService.canAccessProject(project, organizations, List.of(), actor("USR-LEAD", "org-lead"), ProjectPermission.VIEW_PROJECT)).isTrue();
        assertThat(authorizationService.canAccessProject(project, organizations, List.of(), actor("USR-CUSTOMER", "org-customer"), ProjectPermission.VIEW_PROJECT)).isTrue();
        assertThat(authorizationService.canAccessProject(project, organizations, List.of(), actor("USR-SUPPLIER", "org-supplier"), ProjectPermission.VIEW_PROJECT)).isTrue();
        assertThat(authorizationService.canAccessProject(project, organizations, List.of(), actor("USR-PARTNER", "org-partner"), ProjectPermission.VIEW_PROJECT)).isTrue();
    }

    @Test
    void shouldAllowProjectManagerToEditProjectAndMilestones() {
        List<ProjectMemberAggregate> members = List.of(member("USR-PM", "org-lead", ProjectMemberRole.PROJECT_MANAGER));

        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-PM", "org-lead"), ProjectPermission.EDIT_PROJECT)).isTrue();
        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-PM", "org-lead"), ProjectPermission.EDIT_MILESTONE)).isTrue();
    }

    @Test
    void shouldAllowCoordinatorFromSupplierOrganizationToManageProject() {
        List<ProjectMemberAggregate> members = List.of(member("USR-COORD", "org-supplier", ProjectMemberRole.COORDINATOR));

        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-COORD", "org-supplier"), ProjectPermission.EDIT_PROJECT)).isTrue();
        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-COORD", "org-supplier"), ProjectPermission.EDIT_MILESTONE)).isTrue();
    }

    @Test
    void shouldBlockViewerFromEditingProjectOrMilestones() {
        List<ProjectMemberAggregate> members = List.of(member("USR-VIEW", "org-partner", ProjectMemberRole.VIEWER));

        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-VIEW", "org-partner"), ProjectPermission.EDIT_PROJECT)).isFalse();
        assertThat(authorizationService.canAccessProject(project, organizations, members, actor("USR-VIEW", "org-partner"), ProjectPermission.EDIT_MILESTONE)).isFalse();
    }

    @Test
    void shouldBlockCrossTenantProjectAccess() {
        AuthenticatedUser actor = new AuthenticatedUser(
                "sub-foreign",
                "foreign",
                Set.of(Role.MEMBER),
                Set.of(),
                "USR-FOREIGN",
                "MBR-FOREIGN",
                "tenant-b",
                "org-lead",
                null,
                TenantType.EXTERNAL);

        assertThat(authorizationService.canAccessProject(project, organizations, List.of(), actor, ProjectPermission.VIEW_PROJECT)).isFalse();
    }

    private ProjectMemberAggregate member(String userId, String organizationId, ProjectMemberRole role) {
        return new ProjectMemberAggregate("PM-" + userId, "PRJ-1", userId, organizationId, role, true, Instant.now());
    }

    private AuthenticatedUser actor(String userId, String organizationId) {
        return new AuthenticatedUser(
                "sub-" + userId,
                userId,
                Set.of(Role.MEMBER),
                Set.of(),
                userId,
                "MBR-" + userId,
                "tenant-a",
                organizationId,
                null,
                TenantType.EXTERNAL);
    }
}







