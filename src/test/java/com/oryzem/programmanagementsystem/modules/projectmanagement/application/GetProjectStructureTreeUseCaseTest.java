package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetProjectStructureTreeUseCaseTest {

    @Test
    void shouldHideInvisibleSubtreeFromStructureTree() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectTemplateRepository templateRepository = mock(ProjectTemplateRepository.class);
        ProjectStructureLevelTemplateRepository levelRepository = mock(ProjectStructureLevelTemplateRepository.class);
        ProjectStructureNodeRepository nodeRepository = mock(ProjectStructureNodeRepository.class);
        GetProjectStructureTreeUseCase useCase = new GetProjectStructureTreeUseCase(
                authorizationService,
                templateRepository,
                levelRepository,
                nodeRepository,
                new ProjectViewMapper());

        AuthenticatedUser actor = actor("USR-PARTNER", "org-partner");
        ProjectAuthorizationService.ProjectAccess access =
                new ProjectAuthorizationService.ProjectAccess(project(), organizations(), members());
        when(authorizationService.authorizeProject("PRJ-1", actor, com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission.VIEW_PROJECT))
                .thenReturn(access);
        when(templateRepository.findById("TMP-1"))
                .thenReturn(Optional.of(new ProjectTemplateAggregate(
                        "TMP-1",
                        "Template",
                        ProjectFrameworkType.APQP,
                        1,
                        ProjectTemplateStatus.ACTIVE,
                        "PST-1",
                        "org-lead",
                        true,
                        Instant.now())));
        when(levelRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc("PST-1"))
                .thenReturn(List.of(new ProjectStructureLevelTemplateAggregate(
                        "LVL-1", "PST-1", 1, "Project", "PROJECT", true, true, true)));
        ProjectStructureNodeAggregate root = node("ROOT", null, 1, ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-lead", null);
        ProjectStructureNodeAggregate visibleChild = node("VISIBLE-CHILD", "ROOT", 2, ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-partner", null);
        ProjectStructureNodeAggregate hiddenParent = node("HIDDEN-PARENT", "ROOT", 3, ProjectVisibilityScope.LEAD_ONLY, "org-lead", null);
        ProjectStructureNodeAggregate visibleGrandchild = node("VISIBLE-GRANDCHILD", "HIDDEN-PARENT", 4, ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS, "org-partner", "USR-PARTNER");
        when(nodeRepository.findAllByProjectIdOrderBySequenceNoAscIdAsc("PRJ-1"))
                .thenReturn(List.of(root, visibleChild, hiddenParent, visibleGrandchild));
        when(authorizationService.canAccessStructureNode(access.project(), access.organizations(), access.members(),
                root, actor, com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission.VIEW_PROJECT))
                .thenReturn(true);
        when(authorizationService.canAccessStructureNode(access.project(), access.organizations(), access.members(),
                visibleChild, actor, com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission.VIEW_PROJECT))
                .thenReturn(true);
        when(authorizationService.canAccessStructureNode(access.project(), access.organizations(), access.members(),
                hiddenParent, actor, com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission.VIEW_PROJECT))
                .thenReturn(false);
        when(authorizationService.canAccessStructureNode(access.project(), access.organizations(), access.members(),
                visibleGrandchild, actor, com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission.VIEW_PROJECT))
                .thenReturn(true);

        StructureReadModels.ProjectStructureTreeReadModel tree = useCase.execute("PRJ-1", actor);

        assertThat(tree.nodes()).extracting(StructureReadModels.ProjectStructureNodeReadModel::id)
                .containsExactly("ROOT", "VISIBLE-CHILD");
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

    private ProjectStructureNodeAggregate node(
            String id,
            String parentId,
            int sequence,
            ProjectVisibilityScope visibilityScope,
            String ownerOrganizationId,
            String responsibleUserId) {
        return new ProjectStructureNodeAggregate(
                id,
                "PRJ-1",
                "LVL-1",
                parentId,
                id,
                id,
                sequence,
                ownerOrganizationId,
                responsibleUserId,
                ProjectStructureNodeStatus.PLANNED,
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












