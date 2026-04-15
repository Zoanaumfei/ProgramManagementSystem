package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ListProjectMilestonesUseCaseTest {

    @Test
    void shouldReturnOnlyMilestonesVisibleToActor() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectMilestoneRepository milestoneRepository = mock(ProjectMilestoneRepository.class);
        ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();
        ProjectMilestoneAccessPolicy milestoneAccessPolicy =
                new ProjectMilestoneAccessPolicy(new ProjectAccessPolicy(visibilityPolicy), visibilityPolicy);
        ListProjectMilestonesUseCase useCase = new ListProjectMilestonesUseCase(
                authorizationService,
                milestoneRepository,
                milestoneAccessPolicy,
                new ProjectViewMapper());

        AuthenticatedUser actor = actor("USR-PARTNER", "org-partner");
        ProjectAuthorizationService.ProjectAccess access =
                new ProjectAuthorizationService.ProjectAccess(project(), organizations(), members());
        when(authorizationService.authorizeProject("PRJ-1", actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(access);
        when(milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc("PRJ-1"))
                .thenReturn(List.of(
                        milestone("MS-1", 1, ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-partner"),
                        milestone("MS-2", 2, ProjectVisibilityScope.LEAD_ONLY, "org-lead"),
                        milestone("MS-3", 3, ProjectVisibilityScope.INTERNAL_ONLY, "org-partner")));
        when(authorizationService.canAccessMilestone(access.project(), access.organizations(), access.members(),
                milestone("MS-1", 1, ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-partner"), actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(true);
        when(authorizationService.canAccessMilestone(access.project(), access.organizations(), access.members(),
                milestone("MS-2", 2, ProjectVisibilityScope.LEAD_ONLY, "org-lead"), actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(false);
        when(authorizationService.canAccessMilestone(access.project(), access.organizations(), access.members(),
                milestone("MS-3", 3, ProjectVisibilityScope.INTERNAL_ONLY, "org-partner"), actor, ProjectPermission.VIEW_MILESTONE))
                .thenReturn(false);

        List<ProjectReadModels.ProjectMilestoneListReadModel> result = useCase.execute("PRJ-1", null, actor);

        assertThat(result).extracting(ProjectReadModels.ProjectMilestoneListReadModel::id)
                .containsExactly("MS-1");
    }

    private ProjectAggregate project() {
        return new ProjectAggregate(
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
    }

    private List<ProjectOrganizationAggregate> organizations() {
        return List.of(
                new ProjectOrganizationAggregate("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-CUSTOMER", "PRJ-1", "org-customer", ProjectOrganizationRoleType.CUSTOMER, Instant.now(), true),
                new ProjectOrganizationAggregate("PO-PARTNER", "PRJ-1", "org-partner", ProjectOrganizationRoleType.PARTNER, Instant.now(), true));
    }

    private List<ProjectMemberAggregate> members() {
        return List.of(new ProjectMemberAggregate("PM-1", "PRJ-1", "USR-PARTNER", "org-partner", ProjectMemberRole.VIEWER, true, Instant.now()));
    }

    private ProjectMilestoneAggregate milestone(
            String id,
            int sequence,
            ProjectVisibilityScope visibilityScope,
            String ownerOrganizationId) {
        return new ProjectMilestoneAggregate(
                id,
                "PRJ-1",
                "PRJ-1-ROOT",
                "PH-1",
                "MS-" + sequence,
                "Milestone " + sequence,
                sequence,
                LocalDate.now().plusDays(sequence),
                null,
                ProjectMilestoneStatus.NOT_STARTED,
                ownerOrganizationId,
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
}









