package com.oryzem.programmanagementsystem.modules.projectmanagement.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectAuthorizationService;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectViewMapper;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectViews;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ListProjectSummariesQueryTest {

    @Test
    void shouldListAllProjectsForInternalAdmin() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectOrganizationRepository organizationRepository = mock(ProjectOrganizationRepository.class);
        ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectViewMapper viewMapper = mock(ProjectViewMapper.class);
        ListProjectSummariesQuery query = new ListProjectSummariesQuery(
                projectRepository,
                organizationRepository,
                memberRepository,
                authorizationService,
                viewMapper);

        AuthenticatedUser actor = new AuthenticatedUser(
                "sub-internal-admin",
                "internal.admin",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-ADMIN-001",
                "MBR-ADMIN-001",
                "internal-core",
                "internal-core",
                null,
                TenantType.INTERNAL);
        ProjectAggregate tenantAProject = project("PRJ-A", "tenant-a", "org-a");
        ProjectAggregate tenantBProject = project("PRJ-B", "tenant-b", "org-b");
        when(projectRepository.findAllOrderByCreatedAtDescIdDesc()).thenReturn(List.of(tenantAProject, tenantBProject));
        when(authorizationService.canAccessProject(tenantAProject, List.of(), List.of(), actor, ProjectPermission.VIEW_PROJECT)).thenReturn(true);
        when(authorizationService.canAccessProject(tenantBProject, List.of(), List.of(), actor, ProjectPermission.VIEW_PROJECT)).thenReturn(true);
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-A")).thenReturn(List.of());
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-B")).thenReturn(List.of());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-A")).thenReturn(List.of());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-B")).thenReturn(List.of());
        when(viewMapper.toSummary(tenantAProject)).thenReturn(new ProjectViews.ProjectSummaryView(
                tenantAProject.id(),
                tenantAProject.code(),
                tenantAProject.name(),
                tenantAProject.frameworkType(),
                tenantAProject.status(),
                tenantAProject.visibilityScope(),
                tenantAProject.leadOrganizationId(),
                tenantAProject.customerOrganizationId(),
                tenantAProject.plannedStartDate(),
                tenantAProject.plannedEndDate(),
                tenantAProject.updatedAt()));
        when(viewMapper.toSummary(tenantBProject)).thenReturn(new ProjectViews.ProjectSummaryView(
                tenantBProject.id(),
                tenantBProject.code(),
                tenantBProject.name(),
                tenantBProject.frameworkType(),
                tenantBProject.status(),
                tenantBProject.visibilityScope(),
                tenantBProject.leadOrganizationId(),
                tenantBProject.customerOrganizationId(),
                tenantBProject.plannedStartDate(),
                tenantBProject.plannedEndDate(),
                tenantBProject.updatedAt()));

        List<ProjectViews.ProjectSummaryView> result = query.execute(actor);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectViews.ProjectSummaryView::id).containsExactly("PRJ-B", "PRJ-A");
        verify(projectRepository).findAllOrderByCreatedAtDescIdDesc();
    }

    @Test
    void shouldKeepTenantScopedListingForExternalActors() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectOrganizationRepository organizationRepository = mock(ProjectOrganizationRepository.class);
        ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectViewMapper viewMapper = mock(ProjectViewMapper.class);
        ListProjectSummariesQuery query = new ListProjectSummariesQuery(
                projectRepository,
                organizationRepository,
                memberRepository,
                authorizationService,
                viewMapper);

        AuthenticatedUser actor = new AuthenticatedUser(
                "sub-external-admin",
                "tenant.admin",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-EXT-ADMIN-001",
                "MBR-EXT-ADMIN-001",
                "tenant-a",
                "org-a",
                null,
                TenantType.EXTERNAL);
        ProjectAggregate tenantProject = project("PRJ-A", "tenant-a", "org-a");
        when(projectRepository.findAllByTenantIdOrderByCreatedAtDescIdDesc("tenant-a")).thenReturn(List.of(tenantProject));
        when(organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc("PRJ-A")).thenReturn(List.of());
        when(memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc("PRJ-A")).thenReturn(List.of());
        when(authorizationService.canAccessProject(tenantProject, List.of(), List.of(), actor, ProjectPermission.VIEW_PROJECT)).thenReturn(true);
        when(viewMapper.toSummary(tenantProject)).thenReturn(new ProjectViews.ProjectSummaryView(
                tenantProject.id(),
                tenantProject.code(),
                tenantProject.name(),
                tenantProject.frameworkType(),
                tenantProject.status(),
                tenantProject.visibilityScope(),
                tenantProject.leadOrganizationId(),
                tenantProject.customerOrganizationId(),
                tenantProject.plannedStartDate(),
                tenantProject.plannedEndDate(),
                tenantProject.updatedAt()));

        List<ProjectViews.ProjectSummaryView> result = query.execute(actor);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("PRJ-A");
        verify(projectRepository).findAllByTenantIdOrderByCreatedAtDescIdDesc("tenant-a");
    }

    private ProjectAggregate project(String id, String tenantId, String leadOrganizationId) {
        Instant createdAt = "tenant-b".equals(tenantId)
                ? Instant.parse("2026-04-15T12:00:00Z")
                : Instant.parse("2026-04-10T12:00:00Z");
        return new ProjectAggregate(
                id,
                tenantId,
                id + "-CODE",
                "Project " + id,
                "desc",
                "APQP",
                "TMP-1",
                1,
                leadOrganizationId,
                null,
                ProjectStatus.PLANNED,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-05-01"),
                null,
                null,
                "USR-CREATOR",
                createdAt,
                createdAt,
                0L);
    }
}
