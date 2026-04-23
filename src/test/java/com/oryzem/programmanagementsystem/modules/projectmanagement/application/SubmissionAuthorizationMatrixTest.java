package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.DeliverableSubmissionEntity;
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

class SubmissionAuthorizationMatrixTest {

    private ProjectAuthorizationService authorizationService;
    private ProjectAggregate project;
    private ProjectDeliverableAggregate deliverable;
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
        deliverable = new ProjectDeliverableAggregate(
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
                Instant.now(),
                null,
                ProjectDeliverableStatus.SUBMITTED,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER,
                0L);
        organizations = List.of(
                new ProjectOrganizationAggregate("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-CUSTOMER", "PRJ-1", "org-customer", ProjectOrganizationRoleType.CUSTOMER, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-SUPPLIER", "PRJ-1", "org-supplier", ProjectOrganizationRoleType.SUPPLIER, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-PARTNER", "PRJ-1", "org-partner", ProjectOrganizationRoleType.PARTNER, Instant.now(), true));
    }

    @Test
    void shouldAllowApproverToReviewApproveAndRejectSubmission() {
        DeliverableSubmissionAggregate submission = submission(DeliverableSubmissionStatus.SUBMITTED);
        AuthenticatedUser actor = actor("USR-CUS-APP", "org-customer");

        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.REVIEW_SUBMISSION)).isTrue();
        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.APPROVE_SUBMISSION)).isTrue();
        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.REJECT_SUBMISSION)).isTrue();
        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.VIEW_DOCUMENT)).isTrue();
    }

    @Test
    void shouldAllowResponsibleToAttachButNotApproveWhileSubmissionIsOpen() {
        DeliverableSubmissionAggregate submission = submission(DeliverableSubmissionStatus.UNDER_REVIEW);
        AuthenticatedUser actor = actor("USR-SUP-RESP", "org-supplier");

        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.UPLOAD_DOCUMENT)).isTrue();
        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.APPROVE_SUBMISSION)).isFalse();
    }

    @Test
    void shouldBlockPartnerViewerFromReviewingSubmission() {
        DeliverableSubmissionAggregate submission = submission(DeliverableSubmissionStatus.SUBMITTED);
        List<ProjectMemberAggregate> members = List.of(member("USR-PARTNER-VIEW", "org-partner", ProjectMemberRole.VIEWER));
        AuthenticatedUser actor = actor("USR-PARTNER-VIEW", "org-partner");

        assertThat(authorizationService.canAccessSubmission(project, organizations, members, deliverable, submission, actor, ProjectPermission.REVIEW_SUBMISSION)).isFalse();
        assertThat(authorizationService.canAccessSubmission(project, organizations, members, deliverable, submission, actor, ProjectPermission.VIEW_DOCUMENT)).isFalse();
    }

    @Test
    void shouldBlockDocumentUploadsWhenSubmissionIsClosed() {
        DeliverableSubmissionAggregate submission = submission(DeliverableSubmissionStatus.APPROVED);
        AuthenticatedUser actor = actor("USR-SUP-RESP", "org-supplier");

        assertThat(authorizationService.canAccessSubmission(project, organizations, List.of(), deliverable, submission, actor, ProjectPermission.UPLOAD_DOCUMENT)).isFalse();
    }

    private DeliverableSubmissionAggregate submission(DeliverableSubmissionStatus status) {
        return new DeliverableSubmissionAggregate(
                "SUB-1",
                "DLV-1",
                1,
                "USR-SUP-RESP",
                "org-supplier",
                Instant.now(),
                status,
                null,
                null,
                null,
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







