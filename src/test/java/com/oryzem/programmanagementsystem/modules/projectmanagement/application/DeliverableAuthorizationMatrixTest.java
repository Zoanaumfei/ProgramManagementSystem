package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectDeliverableEntity;
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

class DeliverableAuthorizationMatrixTest {

    private ProjectAuthorizationService authorizationService;
    private ProjectAggregate project;
    private List<ProjectOrganizationAggregate> organizations;

    @BeforeEach
    void setUp() {
        AccessContextService accessContextService = mock(AccessContextService.class);
        when(accessContextService.canonicalTenantId("tenant-a")).thenReturn("tenant-a");
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        ProjectAccessPolicy projectAccessPolicy = new ProjectAccessPolicy(visibilityPolicy);
        ProjectDeliverableAccessPolicy deliverableAccessPolicy = new ProjectDeliverableAccessPolicy(visibilityPolicy);
        authorizationService = new ProjectAuthorizationService(
                new ProjectManagementProperties(),
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
        project = new ProjectAggregate(
                "PRJ-1",
                "tenant-a",
                "PRJ-001",
                "Project",
                "desc",
                ProjectFrameworkType.APQP,
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
        organizations = List.of(
                new ProjectOrganizationAggregate("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-CUSTOMER", "PRJ-1", "org-customer", ProjectOrganizationRoleType.CUSTOMER, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-SUPPLIER", "PRJ-1", "org-supplier", ProjectOrganizationRoleType.SUPPLIER, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-PARTNER", "PRJ-1", "org-partner", ProjectOrganizationRoleType.PARTNER, Instant.now(), true));
    }

    @Test
    void shouldAllowResponsibleSupplierToEditSubmitAndAttachDocuments() {
        ProjectDeliverableAggregate deliverable = deliverable(ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER);

        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-SUP-RESP", "org-supplier"), ProjectPermission.EDIT_DELIVERABLE)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-SUP-RESP", "org-supplier"), ProjectPermission.SUBMIT_DELIVERABLE)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-SUP-RESP", "org-supplier"), ProjectPermission.UPLOAD_DOCUMENT)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-SUP-RESP", "org-supplier"), ProjectPermission.VIEW_DOCUMENT)).isTrue();
    }

    @Test
    void shouldAllowApproverCustomerToViewButNotSubmitOrEditDeliverable() {
        ProjectDeliverableAggregate deliverable = deliverable(ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER);

        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-CUS-APP", "org-customer"), ProjectPermission.VIEW_DOCUMENT)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-CUS-APP", "org-customer"), ProjectPermission.EDIT_DELIVERABLE)).isFalse();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, List.of(), deliverable, actor("USR-CUS-APP", "org-customer"), ProjectPermission.SUBMIT_DELIVERABLE)).isFalse();
    }

    @Test
    void shouldAllowPartnerViewerOnlyWhenScopeIsAllParticipants() {
        ProjectDeliverableAggregate visibleDeliverable = deliverable(ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS);
        ProjectDeliverableAggregate hiddenDeliverable = deliverable(ProjectVisibilityScope.LEAD_ONLY);
        List<ProjectMemberAggregate> viewerMembership = List.of(member("USR-PARTNER-VIEW", "org-partner", ProjectMemberRole.VIEWER));
        AuthenticatedUser actor = actor("USR-PARTNER-VIEW", "org-partner");

        assertThat(authorizationService.canAccessDeliverable(project, organizations, viewerMembership, visibleDeliverable, actor, ProjectPermission.VIEW_DELIVERABLE)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, viewerMembership, hiddenDeliverable, actor, ProjectPermission.VIEW_DELIVERABLE)).isFalse();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, viewerMembership, visibleDeliverable, actor, ProjectPermission.UPLOAD_DOCUMENT)).isFalse();
    }

    @Test
    void shouldAllowLeadProjectManagerToBypassScopedVisibilityAndManageDeliverable() {
        ProjectDeliverableAggregate deliverable = deliverable(ProjectVisibilityScope.LEAD_ONLY);
        List<ProjectMemberAggregate> members = List.of(member("USR-LEAD-PM", "org-lead", ProjectMemberRole.PROJECT_MANAGER));
        AuthenticatedUser actor = actor("USR-LEAD-PM", "org-lead");

        assertThat(authorizationService.canAccessDeliverable(project, organizations, members, deliverable, actor, ProjectPermission.VIEW_DOCUMENT)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, members, deliverable, actor, ProjectPermission.EDIT_DELIVERABLE)).isTrue();
        assertThat(authorizationService.canAccessDeliverable(project, organizations, members, deliverable, actor, ProjectPermission.SUBMIT_DELIVERABLE)).isTrue();
    }

    private ProjectDeliverableAggregate deliverable(ProjectVisibilityScope visibilityScope) {
        return new ProjectDeliverableAggregate(
                "DLV-1",
                "PRJ-1",
                "PRJ-1-ROOT",
                null,
                null,
                "D-001",
                "Evidence",
                "desc",
                DeliverableType.DOCUMENT_PACKAGE,
                "org-supplier",
                "USR-SUP-RESP",
                "org-customer",
                "USR-CUS-APP",
                true,
                LocalDate.now().plusDays(5),
                null,
                null,
                ProjectDeliverableStatus.READY_FOR_SUBMISSION,
                com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority.HIGH,
                visibilityScope,
                0L);
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







