package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectStructureNodeAuthorizationTest {

    private ProjectAuthorizationService authorizationService;
    private ProjectRepository projectRepository;
    private ProjectOrganizationRepository organizationRepository;
    private ProjectMemberRepository memberRepository;
    private ProjectStructureNodeRepository structureNodeRepository;
    private ProjectMilestoneRepository milestoneRepository;
    private ProjectDeliverableRepository deliverableRepository;
    private DeliverableSubmissionRepository submissionRepository;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        organizationRepository = mock(ProjectOrganizationRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        structureNodeRepository = mock(ProjectStructureNodeRepository.class);
        milestoneRepository = mock(ProjectMilestoneRepository.class);
        deliverableRepository = mock(ProjectDeliverableRepository.class);
        submissionRepository = mock(DeliverableSubmissionRepository.class);
        AccessContextService accessContextService = mock(AccessContextService.class);
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        ProjectAccessPolicy projectAccessPolicy = new ProjectAccessPolicy(visibilityPolicy);
        when(accessContextService.canonicalTenantId("tenant-a")).thenReturn("tenant-a");
        when(accessContextService.canonicalTenantId("TEN-internal-core")).thenReturn("TEN-internal-core");
        authorizationService = new ProjectAuthorizationService(
                new ProjectManagementProperties(),
                projectRepository,
                organizationRepository,
                memberRepository,
                deliverableRepository,
                milestoneRepository,
                structureNodeRepository,
                submissionRepository,
                accessContextService,
                mock(ProjectAuditService.class),
                projectAccessPolicy,
                new ProjectMilestoneAccessPolicy(projectAccessPolicy, visibilityPolicy),
                new ProjectDeliverableAccessPolicy(visibilityPolicy),
                new DeliverableSubmissionAccessPolicy(new ProjectDeliverableAccessPolicy(visibilityPolicy)),
                new ProjectStructureNodeAccessPolicy(projectAccessPolicy, visibilityPolicy));
    }

    @Test
    void shouldAuthorizeInternalAdminForRestrictiveStructureNode() {
        ProjectAggregate project = project();
        ProjectStructureNodeAggregate node = node(ProjectVisibilityScope.INTERNAL_ONLY);
        when(projectRepository.findById("PRJ-66C2BCF24E35")).thenReturn(Optional.of(project));
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(organizations());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(List.of());
        when(structureNodeRepository.findByIdAndProjectId("NODE-1", "PRJ-66C2BCF24E35"))
                .thenReturn(Optional.of(node));

        assertThatCode(() -> authorizationService.authorizeStructureNode(
                "PRJ-66C2BCF24E35",
                "NODE-1",
                internalAdmin(),
                ProjectPermission.VIEW_PROJECT))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAuthorizeInternalSupportForRestrictiveMilestone() {
        ProjectAggregate project = project();
        ProjectMilestoneAggregate milestone = milestone(ProjectVisibilityScope.INTERNAL_ONLY);
        when(projectRepository.findById("PRJ-66C2BCF24E35")).thenReturn(Optional.of(project));
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(organizations());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(List.of());
        when(milestoneRepository.findByIdAndProjectId("MS-1", "PRJ-66C2BCF24E35"))
                .thenReturn(Optional.of(milestone));

        assertThatCode(() -> authorizationService.authorizeMilestone(
                "PRJ-66C2BCF24E35",
                "MS-1",
                internalSupport(),
                ProjectPermission.EDIT_MILESTONE))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAuthorizeInternalSupportForRestrictiveSubmission() {
        ProjectAggregate project = project();
        ProjectDeliverableAggregate deliverable = deliverable(ProjectVisibilityScope.INTERNAL_ONLY);
        DeliverableSubmissionAggregate submission = submission();
        when(projectRepository.findById("PRJ-66C2BCF24E35")).thenReturn(Optional.of(project));
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(organizations());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-66C2BCF24E35"))
                .thenReturn(List.of());
        when(deliverableRepository.findByIdAndProjectId("DLV-1", "PRJ-66C2BCF24E35"))
                .thenReturn(Optional.of(deliverable));
        when(submissionRepository.findByIdAndDeliverableId("SUB-1", "DLV-1"))
                .thenReturn(Optional.of(submission));

        assertThatCode(() -> authorizationService.authorizeSubmission(
                "PRJ-66C2BCF24E35",
                "DLV-1",
                "SUB-1",
                internalSupport(),
                ProjectPermission.APPROVE_SUBMISSION))
                .doesNotThrowAnyException();
    }

    private ProjectAggregate project() {
        return new ProjectAggregate(
                "PRJ-66C2BCF24E35",
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
        return List.of(new ProjectOrganizationAggregate(
                "PO-LEAD",
                "PRJ-66C2BCF24E35",
                "org-lead",
                ProjectOrganizationRoleType.LEAD,
                Instant.now(),
                true));
    }

    private ProjectStructureNodeAggregate node(ProjectVisibilityScope visibilityScope) {
        return new ProjectStructureNodeAggregate(
                "NODE-1",
                "PRJ-66C2BCF24E35",
                "LVL-1",
                "PRJ-66C2BCF24E35-ROOT",
                "Restricted node",
                "NODE-1",
                1,
                "org-lead",
                "USR-RESP",
                ProjectStructureNodeStatus.ACTIVE,
                visibilityScope,
                0L);
    }

    private ProjectMilestoneAggregate milestone(ProjectVisibilityScope visibilityScope) {
        return new ProjectMilestoneAggregate(
                "MS-1",
                "PRJ-66C2BCF24E35",
                "NODE-1",
                "PH-1",
                "MS-1",
                "Restricted milestone",
                1,
                LocalDate.now().plusDays(10),
                null,
                ProjectMilestoneStatus.NOT_STARTED,
                "org-lead",
                visibilityScope,
                0L);
    }

    private ProjectDeliverableAggregate deliverable(ProjectVisibilityScope visibilityScope) {
        return new ProjectDeliverableAggregate(
                "DLV-1",
                "PRJ-66C2BCF24E35",
                "NODE-1",
                "PH-1",
                "MS-1",
                "DLV-1",
                "Restricted deliverable",
                "desc",
                DeliverableType.DOCUMENT_PACKAGE,
                "org-lead",
                "USR-RESP",
                "org-customer",
                "USR-APP",
                true,
                LocalDate.now().plusDays(20),
                Instant.now(),
                null,
                ProjectDeliverableStatus.SUBMITTED,
                ProjectPriority.HIGH,
                visibilityScope,
                0L);
    }

    private DeliverableSubmissionAggregate submission() {
        return new DeliverableSubmissionAggregate(
                "SUB-1",
                "DLV-1",
                1,
                "USR-RESP",
                "org-lead",
                Instant.now(),
                DeliverableSubmissionStatus.SUBMITTED,
                null,
                null,
                null,
                0L);
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

    private AuthenticatedUser internalSupport() {
        return new AuthenticatedUser(
                "sub-support",
                "support@oryzem.com",
                Set.of(Role.SUPPORT),
                Set.of(),
                "USR-INTERNAL-SUPPORT",
                "MBR-INTERNAL-SUPPORT",
                "TEN-internal-core",
                "ORG-INTERNAL",
                null,
                TenantType.INTERNAL);
    }
}
