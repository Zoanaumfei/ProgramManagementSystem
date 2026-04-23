package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPublicFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
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
import org.mockito.ArgumentCaptor;

class SubmitDeliverableUseCaseTest {

    @Test
    void shouldAttachOnlySubmissionContextDocumentsWhenCreatingSubmission() {
        ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
        ProjectDeliverableRepository deliverableRepository = mock(ProjectDeliverableRepository.class);
        DeliverableSubmissionRepository submissionRepository = mock(DeliverableSubmissionRepository.class);
        DeliverableSubmissionDocumentRepository submissionDocumentRepository = mock(DeliverableSubmissionDocumentRepository.class);
        ProjectViewMapper viewMapper = new ProjectViewMapper();
        ProjectIdempotencyService idempotencyService = mock(ProjectIdempotencyService.class);
        DocumentPublicFacade documentPublicFacade = mock(DocumentPublicFacade.class);
        ProjectDocumentValidationService validationService = new ProjectDocumentValidationService(documentPublicFacade);
        Clock clock = Clock.fixed(Instant.parse("2026-04-11T12:00:00Z"), ZoneOffset.UTC);

        SubmitDeliverableUseCase useCase = new SubmitDeliverableUseCase(
                authorizationService,
                deliverableRepository,
                submissionRepository,
                submissionDocumentRepository,
                viewMapper,
                idempotencyService,
                validationService,
                clock);

        AuthenticatedUser actor = actor();
        ProjectAuthorizationService.DeliverableAccess access = deliverableAccess();
        DeliverableSubmissionAggregate persistedSubmission = new DeliverableSubmissionAggregate(
                "PRJSUB-001",
                access.deliverable().id(),
                1,
                actor.userId(),
                actor.organizationId(),
                Instant.parse("2026-04-11T12:00:00Z"),
                DeliverableSubmissionStatus.SUBMITTED,
                null,
                null,
                null,
                0L);

        when(authorizationService.authorizeDeliverable("PRJ-001", "DLV-001", actor, ProjectPermission.SUBMIT_DELIVERABLE))
                .thenReturn(access);
        when(idempotencyService.execute(
                eq("tenant-a"),
                eq("SUBMIT_DELIVERABLE"),
                eq("idem-1"),
                any(),
                eq(SubmissionViews.DeliverableSubmissionView.class),
                any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<SubmissionViews.DeliverableSubmissionView>>getArgument(5).get());
        when(submissionRepository.existsByDeliverableIdAndStatusIn(eq("DLV-001"), any())).thenReturn(false);
        when(submissionRepository.findTopByDeliverableIdOrderBySubmissionNumberDesc("DLV-001")).thenReturn(Optional.empty());
        when(submissionRepository.save(any(DeliverableSubmissionAggregate.class))).thenReturn(persistedSubmission);
        when(documentPublicFacade.getAccessibleDocument("DOC-1", actor)).thenReturn(documentView("DOC-1", "PRJSUB-001"));
        when(submissionDocumentRepository.findAllBySubmissionId("PRJSUB-001"))
                .thenReturn(List.of(new DeliverableSubmissionDocumentAggregate("PRJSDOC-001", "PRJSUB-001", "DOC-1")));

        SubmissionViews.DeliverableSubmissionView response = useCase.execute(
                "PRJ-001",
                "DLV-001",
                new SubmitDeliverableUseCase.SubmitDeliverableCommand(0L, List.of("DOC-1")),
                actor,
                "idem-1");

        ArgumentCaptor<DeliverableSubmissionDocumentAggregate> documentCaptor = ArgumentCaptor.forClass(DeliverableSubmissionDocumentAggregate.class);
        verify(submissionDocumentRepository).save(documentCaptor.capture());
        verify(deliverableRepository).save(access.deliverable().markSubmitted(Instant.parse("2026-04-11T12:00:00Z")));

        assertThat(documentCaptor.getValue().submissionId()).isEqualTo("PRJSUB-001");
        assertThat(documentCaptor.getValue().documentId()).isEqualTo("DOC-1");
        assertThat(response.id()).isEqualTo("PRJSUB-001");
        assertThat(response.documentIds()).containsExactly("DOC-1");
    }

    private ProjectAuthorizationService.DeliverableAccess deliverableAccess() {
        Instant now = Instant.parse("2026-04-11T12:00:00Z");
        ProjectAggregate project = new ProjectAggregate(
                "PRJ-001",
                "tenant-a",
                "PRJ-001",
                "Project 001",
                "test",
                "APQP",
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
                "PRJ-001-ROOT",
                "PH-001",
                "MS-001",
                "DLV-001",
                "Deliverable 001",
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
                ProjectDeliverableStatus.READY_FOR_SUBMISSION,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                0L);
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

    private DocumentView documentView(String id, String submissionId) {
        Instant now = Instant.parse("2026-04-11T12:00:00Z");
        return new DocumentView(
                id,
                "tenant-a",
                DocumentContextType.PROJECT_DELIVERABLE_SUBMISSION,
                submissionId,
                "tenant-a",
                "submission.pdf",
                "submission.pdf",
                "application/pdf",
                "pdf",
                1024L,
                "checksum",
                DocumentStatus.ACTIVE,
                "USR-1",
                "tenant-a",
                now,
                now,
                null);
    }
}

