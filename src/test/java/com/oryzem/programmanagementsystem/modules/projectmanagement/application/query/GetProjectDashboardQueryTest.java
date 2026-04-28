package com.oryzem.programmanagementsystem.modules.projectmanagement.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.*;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetProjectDashboardQueryTest {

    @Test
    void shouldCountOnlyVisibleMilestonesAndDeliverables() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectDeliverableRepository deliverableRepository = mock(ProjectDeliverableRepository.class);
        ProjectMilestoneRepository milestoneRepository = mock(ProjectMilestoneRepository.class);
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        GetProjectDashboardQuery query = new GetProjectDashboardQuery(
                authorizationService,
                deliverableRepository,
                milestoneRepository,
                new ProjectDeliverableAccessPolicy(visibilityPolicy),
                new ProjectMilestoneAccessPolicy(new ProjectAccessPolicy(visibilityPolicy), visibilityPolicy),
                Clock.fixed(Instant.parse("2026-04-12T12:00:00Z"), ZoneOffset.UTC));

        AuthenticatedUser actor = actor("USR-SUPPLIER", "org-supplier");
        ProjectAuthorizationService.ProjectAccess access =
                new ProjectAuthorizationService.ProjectAccess(project(), organizations(), members());
        when(authorizationService.authorizeProject("PRJ-1", actor, ProjectPermission.VIEW_PROJECT))
                .thenReturn(access);
        when(deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc("PRJ-1"))
                .thenReturn(List.of(
                        deliverable("DLV-1", ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, ProjectDeliverableStatus.READY_FOR_SUBMISSION),
                        deliverable("DLV-2", ProjectVisibilityScope.LEAD_ONLY, ProjectDeliverableStatus.READY_FOR_SUBMISSION)));
        when(milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc("PRJ-1"))
                .thenReturn(List.of(
                        milestone("MS-1", ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-supplier", ProjectMilestoneStatus.AT_RISK),
                        milestone("MS-2", ProjectVisibilityScope.LEAD_ONLY, "org-lead", ProjectMilestoneStatus.AT_RISK)));
        when(authorizationService.canAccessDeliverable(access.project(), access.organizations(), access.members(),
                deliverable("DLV-1", ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, ProjectDeliverableStatus.READY_FOR_SUBMISSION), actor, ProjectPermission.VIEW_DELIVERABLE))
                .thenReturn(true);
        when(authorizationService.canAccessDeliverable(access.project(), access.organizations(), access.members(),
                deliverable("DLV-2", ProjectVisibilityScope.LEAD_ONLY, ProjectDeliverableStatus.READY_FOR_SUBMISSION), actor, ProjectPermission.VIEW_DELIVERABLE))
                .thenReturn(false);
        when(authorizationService.canAccessMilestone(access.project(), access.organizations(), access.members(),
                milestone("MS-1", ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-supplier", ProjectMilestoneStatus.AT_RISK), actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(true);
        when(authorizationService.canAccessMilestone(access.project(), access.organizations(), access.members(),
                milestone("MS-2", ProjectVisibilityScope.LEAD_ONLY, "org-lead", ProjectMilestoneStatus.AT_RISK), actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(false);

        DashboardViews.ProjectDashboardView result = query.execute("PRJ-1", null, actor);

        assertThat(result.totalDeliverables()).isEqualTo(1);
        assertThat(result.pendingSubmissionCount()).isEqualTo(1);
        assertThat(result.milestonesAtRisk()).isEqualTo(1);
    }

    @Test
    void shouldReturnScopedDashboardForInternalAdminOnRestrictiveStructureNode() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectOrganizationRepository organizationRepository = mock(ProjectOrganizationRepository.class);
        ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
        ProjectStructureNodeRepository structureNodeRepository = mock(ProjectStructureNodeRepository.class);
        ProjectDeliverableRepository deliverableRepository = mock(ProjectDeliverableRepository.class);
        ProjectMilestoneRepository milestoneRepository = mock(ProjectMilestoneRepository.class);
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        ProjectAccessPolicy projectAccessPolicy = new ProjectAccessPolicy(visibilityPolicy);
        AccessContextService accessContextService = mock(AccessContextService.class);
        when(accessContextService.canonicalTenantId("TEN-internal-core")).thenReturn("TEN-internal-core");
        ProjectAuthorizationService authorizationService = new ProjectAuthorizationService(
                new ProjectManagementProperties(),
                projectRepository,
                organizationRepository,
                memberRepository,
                deliverableRepository,
                milestoneRepository,
                structureNodeRepository,
                mock(com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository.class),
                accessContextService,
                mock(ProjectAuditService.class),
                projectAccessPolicy,
                new ProjectMilestoneAccessPolicy(projectAccessPolicy, visibilityPolicy),
                new ProjectDeliverableAccessPolicy(visibilityPolicy),
                new DeliverableSubmissionAccessPolicy(new ProjectDeliverableAccessPolicy(visibilityPolicy)),
                new ProjectStructureNodeAccessPolicy(projectAccessPolicy, visibilityPolicy));
        GetProjectDashboardQuery query = new GetProjectDashboardQuery(
                authorizationService,
                deliverableRepository,
                milestoneRepository,
                new ProjectDeliverableAccessPolicy(visibilityPolicy),
                new ProjectMilestoneAccessPolicy(projectAccessPolicy, visibilityPolicy),
                Clock.fixed(Instant.parse("2026-04-12T12:00:00Z"), ZoneOffset.UTC));

        when(projectRepository.findById("PRJ-1")).thenReturn(Optional.of(project()));
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-1"))
                .thenReturn(organizations());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-1"))
                .thenReturn(List.of());
        when(structureNodeRepository.findByIdAndProjectId("NODE-1", "PRJ-1"))
                .thenReturn(Optional.of(structureNode(ProjectVisibilityScope.INTERNAL_ONLY)));
        when(deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc("PRJ-1", "NODE-1"))
                .thenReturn(List.of());
        when(milestoneRepository.findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc("PRJ-1", "NODE-1"))
                .thenReturn(List.of());

        DashboardViews.ProjectDashboardView result = query.execute("PRJ-1", "NODE-1", internalAdmin());

        assertThat(result.projectId()).isEqualTo("PRJ-1");
        assertThat(result.totalDeliverables()).isZero();
    }

    private ProjectAggregate project() {
        return new ProjectAggregate(
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
    }

    private List<ProjectOrganizationAggregate> organizations() {
        return List.of(
                new ProjectOrganizationAggregate("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-SUPPLIER", "PRJ-1", "org-supplier", ProjectOrganizationRoleType.SUPPLIER, Instant.now(), true));
    }

    private List<ProjectMemberAggregate> members() {
        return List.of(new ProjectMemberAggregate("PM-1", "PRJ-1", "USR-SUPPLIER", "org-supplier", ProjectMemberRole.VIEWER, true, Instant.now()));
    }

    private ProjectDeliverableAggregate deliverable(
            String id,
            ProjectVisibilityScope visibilityScope,
            ProjectDeliverableStatus status) {
        return new ProjectDeliverableAggregate(
                id,
                "PRJ-1",
                "PRJ-1-ROOT",
                "PH-1",
                "MS-1",
                id,
                id,
                "desc",
                DeliverableType.DOCUMENT_PACKAGE,
                "org-supplier",
                "USR-SUPPLIER",
                "org-lead",
                "USR-LEAD",
                true,
                LocalDate.parse("2026-04-20"),
                null,
                null,
                status,
                ProjectPriority.HIGH,
                visibilityScope,
                0L);
    }

    private ProjectMilestoneAggregate milestone(
            String id,
            ProjectVisibilityScope visibilityScope,
            String ownerOrganizationId,
            ProjectMilestoneStatus status) {
        return new ProjectMilestoneAggregate(
                id,
                "PRJ-1",
                "PRJ-1-ROOT",
                "PH-1",
                id,
                id,
                1,
                LocalDate.parse("2026-04-20"),
                null,
                status,
                ownerOrganizationId,
                visibilityScope,
                0L);
    }

    private ProjectStructureNodeAggregate structureNode(ProjectVisibilityScope visibilityScope) {
        return new ProjectStructureNodeAggregate(
                "NODE-1",
                "PRJ-1",
                "LVL-1",
                "PRJ-1-ROOT",
                "Restricted node",
                "NODE-1",
                1,
                "org-lead",
                "USR-RESP",
                ProjectStructureNodeStatus.ACTIVE,
                visibilityScope,
                0L);
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

    private AuthenticatedUser internalAdmin() {
        return new AuthenticatedUser(
                "sub-admin",
                "admin@oryzem.com",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-INTERNAL-ADMIN",
                "MBR-INTERNAL-ADMIN",
                "TEN-internal-core",
                "ORG-INTERNAL",
                null,
                TenantType.INTERNAL);
    }
}









