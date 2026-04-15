package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
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

class UpdateDeliverableUseCaseTest {

    @Test
    void shouldPersistUpdatedDeliverableThroughPort() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectDeliverableRepository deliverableRepository = mock(ProjectDeliverableRepository.class);
        ProjectViewMapper viewMapper = new ProjectViewMapper();
        UpdateDeliverableUseCase useCase = new UpdateDeliverableUseCase(authorizationService, deliverableRepository, viewMapper);

        AuthenticatedUser actor = actor();
        ProjectAuthorizationService.DeliverableAccess access = deliverableAccess();

        when(authorizationService.authorizeDeliverable("PRJ-001", "DLV-001", actor, ProjectPermission.EDIT_DELIVERABLE))
                .thenReturn(access);
        when(deliverableRepository.save(access.deliverable().updateOperationalFields(
                "updated description",
                "tenant-b",
                "USR-2",
                "customer-a",
                "USR-APP-2",
                LocalDate.parse("2026-05-15"),
                ProjectDeliverableStatus.WAIVED,
                ProjectPriority.CRITICAL,
                ProjectVisibilityScope.LEAD_ONLY)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectViews.ProjectDeliverableView response = useCase.execute(
                "PRJ-001",
                "DLV-001",
                new UpdateDeliverableUseCase.UpdateDeliverableCommand(
                        "updated description",
                        "tenant-b",
                        "USR-2",
                        "customer-a",
                        "USR-APP-2",
                        LocalDate.parse("2026-05-15"),
                        ProjectDeliverableStatus.WAIVED,
                        ProjectPriority.CRITICAL,
                        ProjectVisibilityScope.LEAD_ONLY,
                        3L),
                actor);

        verify(deliverableRepository).save(access.deliverable().updateOperationalFields(
                "updated description",
                "tenant-b",
                "USR-2",
                "customer-a",
                "USR-APP-2",
                LocalDate.parse("2026-05-15"),
                ProjectDeliverableStatus.WAIVED,
                ProjectPriority.CRITICAL,
                ProjectVisibilityScope.LEAD_ONLY));
        assertThat(response.description()).isEqualTo("updated description");
        assertThat(response.responsibleOrganizationId()).isEqualTo("tenant-b");
        assertThat(response.responsibleUserId()).isEqualTo("USR-2");
        assertThat(response.approverUserId()).isEqualTo("USR-APP-2");
        assertThat(response.plannedDueDate()).isEqualTo(LocalDate.parse("2026-05-15"));
        assertThat(response.status()).isEqualTo(ProjectDeliverableStatus.WAIVED);
        assertThat(response.priority()).isEqualTo(ProjectPriority.CRITICAL);
        assertThat(response.visibilityScope()).isEqualTo(ProjectVisibilityScope.LEAD_ONLY);
    }

    private ProjectAuthorizationService.DeliverableAccess deliverableAccess() {
        Instant now = Instant.parse("2026-04-11T12:00:00Z");
        ProjectAggregate project = new ProjectAggregate(
                "PRJ-001",
                "tenant-a",
                "PRJ-001",
                "Project 001",
                "test",
                ProjectFrameworkType.APQP,
                "TMP-001",
                1,
                "tenant-a",
                "customer-a",
                ProjectStatus.ACTIVE,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-04-01"),
                null,
                "USR-1",
                now,
                now,
                0L);
        ProjectDeliverableAggregate deliverable = new ProjectDeliverableAggregate(
                "DLV-001",
                "PRJ-001",
                "NODE-001",
                "PH-001",
                "MS-001",
                "DLV-001",
                "Deliverable 001",
                "initial description",
                DeliverableType.DOCUMENT_PACKAGE,
                "tenant-a",
                "USR-RESP",
                "customer-a",
                "USR-APP",
                true,
                LocalDate.parse("2026-05-01"),
                null,
                null,
                ProjectDeliverableStatus.READY_FOR_SUBMISSION,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                3L);
        return new ProjectAuthorizationService.DeliverableAccess(project, List.of(), List.of(), deliverable);
    }

    private AuthenticatedUser actor() {
        return new AuthenticatedUser(
                "subject-1",
                "admin.a@tenant.com",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-1",
                "MEM-1",
                "tenant-a",
                "tenant-a",
                null,
                TenantType.EXTERNAL);
    }
}

