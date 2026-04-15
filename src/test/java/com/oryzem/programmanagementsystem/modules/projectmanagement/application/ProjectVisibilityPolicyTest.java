package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectEntity;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.ProjectOrganizationEntity;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectVisibilityPolicyTest {

    private final ProjectVisibilityPolicy visibilityPolicy = new ProjectVisibilityPolicy();

    @Test
    void shouldAllowOwnerOrganizationToViewResponsibleMilestone() {
        assertThat(visibilityPolicy.canViewMilestone(
                        "org-lead",
                        actorContext(actor("USR-SUPPLIER", "org-supplier"), Set.of(ProjectMemberRole.VIEWER)),
                        ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER,
                        "org-supplier"))
                .isTrue();
    }

    @Test
    void shouldBlockParticipantFromLeadOnlyMilestoneWhenNotManagerOrLead() {
        assertThat(visibilityPolicy.canViewMilestone(
                        "org-lead",
                        actorContext(actor("USR-PARTNER", "org-partner"), Set.of(ProjectMemberRole.VIEWER)),
                        ProjectVisibilityScope.LEAD_ONLY,
                        "org-lead"))
                .isFalse();
    }

    @Test
    void shouldAllowResponsibleUserToViewResponsibleStructureNode() {
        assertThat(visibilityPolicy.canViewStructureNode(
                        "org-lead",
                        actorContext(actor("USR-RESP", "org-partner"), Set.of(ProjectMemberRole.VIEWER)),
                        ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER,
                        "org-supplier",
                        "USR-RESP"))
                .isTrue();
    }

    @Test
    void shouldKeepInternalOnlyHiddenFromExternalParticipants() {
        assertThat(visibilityPolicy.canViewStructureNode(
                        "org-lead",
                        actorContext(actor("USR-COORD", "org-lead"), Set.of(ProjectMemberRole.COORDINATOR)),
                        ProjectVisibilityScope.INTERNAL_ONLY,
                        "org-lead",
                        "USR-COORD"))
                .isFalse();
    }

    private ProjectActorContext actorContext(AuthenticatedUser actor, Set<ProjectMemberRole> memberRoles) {
        boolean participant = organizations().stream().anyMatch(org -> org.getOrganizationId().equals(actor.organizationId()));
        boolean manager = memberRoles.contains(ProjectMemberRole.PROJECT_MANAGER) || memberRoles.contains(ProjectMemberRole.COORDINATOR);
        return new ProjectActorContext(actor, participant, memberRoles, manager);
    }

    private ProjectEntity project() {
        return ProjectEntity.create(new ProjectAggregate(
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
                0L));
    }

    private List<ProjectOrganizationEntity> organizations() {
        return List.of(
                ProjectOrganizationEntity.create("PO-LEAD", "PRJ-1", "org-lead", ProjectOrganizationRoleType.LEAD, Instant.now()),
                ProjectOrganizationEntity.create("PO-CUSTOMER", "PRJ-1", "org-customer", ProjectOrganizationRoleType.CUSTOMER, Instant.now()),
                ProjectOrganizationEntity.create("PO-SUPPLIER", "PRJ-1", "org-supplier", ProjectOrganizationRoleType.SUPPLIER, Instant.now()),
                ProjectOrganizationEntity.create("PO-PARTNER", "PRJ-1", "org-partner", ProjectOrganizationRoleType.PARTNER, Instant.now()));
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
