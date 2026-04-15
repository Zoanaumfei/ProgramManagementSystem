package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
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

class ListPendingReviewDeliverablesUseCaseTest {

    @Test
    void shouldLoadPendingReviewDeliverablesThroughPortAndFilterByReviewAccess() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectDeliverableRepository deliverableRepository = mock(ProjectDeliverableRepository.class);
        ProjectViewMapper viewMapper = new ProjectViewMapper();
        ListPendingReviewDeliverablesUseCase useCase = new ListPendingReviewDeliverablesUseCase(
                authorizationService,
                deliverableRepository,
                viewMapper);

        AuthenticatedUser actor = actor();
        ProjectAggregate project = project();
        ProjectAuthorizationService.ProjectAccess access = new ProjectAuthorizationService.ProjectAccess(project, List.of(), List.of());
        ProjectDeliverableAggregate submitted = deliverable("DLV-001", ProjectDeliverableStatus.SUBMITTED);
        ProjectDeliverableAggregate underReview = deliverable("DLV-002", ProjectDeliverableStatus.UNDER_REVIEW);
        ProjectDeliverableAggregate approved = deliverable("DLV-003", ProjectDeliverableStatus.APPROVED);

        when(authorizationService.authorizeProject("PRJ-001", actor, ProjectPermission.VIEW_PROJECT)).thenReturn(access);
        when(deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc("PRJ-001", "NODE-001"))
                .thenReturn(List.of(submitted, underReview, approved));
        when(authorizationService.canAccessDeliverable(project, List.of(), List.of(), submitted, actor, ProjectPermission.REVIEW_SUBMISSION))
                .thenReturn(true);
        when(authorizationService.canAccessDeliverable(project, List.of(), List.of(), underReview, actor, ProjectPermission.REVIEW_SUBMISSION))
                .thenReturn(false);
        when(authorizationService.authorizeStructureNode("PRJ-001", "NODE-001", actor, ProjectPermission.VIEW_PROJECT))
                .thenReturn(new ProjectAuthorizationService.StructureNodeAccess(project, List.of(), List.of(), null));

        List<ProjectReadModels.PendingSubmissionReviewReadModel> response = useCase.execute("PRJ-001", "NODE-001", actor);

        verify(authorizationService).authorizeStructureNode("PRJ-001", "NODE-001", actor, ProjectPermission.VIEW_PROJECT);
        verify(deliverableRepository).findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc("PRJ-001", "NODE-001");
        assertThat(response).extracting(ProjectReadModels.PendingSubmissionReviewReadModel::id).containsExactly("DLV-001");
    }

    private ProjectAggregate project() {
        Instant now = Instant.parse("2026-04-11T12:00:00Z");
        return new ProjectAggregate(
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
    }

    private ProjectDeliverableAggregate deliverable(String id, ProjectDeliverableStatus status) {
        return new ProjectDeliverableAggregate(
                id,
                "PRJ-001",
                "NODE-001",
                "PH-001",
                "MS-001",
                id,
                "Deliverable " + id,
                "test",
                DeliverableType.DOCUMENT_PACKAGE,
                "tenant-a",
                "USR-RESP",
                "customer-a",
                "USR-APP",
                true,
                LocalDate.parse("2026-05-01"),
                null,
                null,
                status,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                0L);
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


