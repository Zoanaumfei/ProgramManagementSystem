package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPublicFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectDocumentValidationServiceTest {

    private final DocumentPublicFacade documentPublicFacade = mock(DocumentPublicFacade.class);
    private final ProjectDocumentValidationService service = new ProjectDocumentValidationService(documentPublicFacade);

    @Test
    void shouldAcceptOnlySubmissionContextDocuments() {
        AuthenticatedUser actor = actor();
        ProjectAuthorizationService.DeliverableAccess access = deliverableAccess();
        DocumentView document = documentView("DOC-1", "tenant-a", DocumentContextType.PROJECT_DELIVERABLE_SUBMISSION, "PRJSUB-001", DocumentStatus.ACTIVE);
        when(documentPublicFacade.getAccessibleDocument("DOC-1", actor)).thenReturn(document);

        List<String> validated = service.validateSubmissionDocuments(access, "PRJSUB-001", List.of("DOC-1"), actor);

        assertThat(validated).containsExactly("DOC-1");
    }

    @Test
    void shouldRejectDeliverableWorkDocumentInFormalSubmission() {
        AuthenticatedUser actor = actor();
        ProjectAuthorizationService.DeliverableAccess access = deliverableAccess();
        DocumentView document = documentView("DOC-1", "tenant-a", DocumentContextType.PROJECT_DELIVERABLE, "DLV-001", DocumentStatus.ACTIVE);
        when(documentPublicFacade.getAccessibleDocument("DOC-1", actor)).thenReturn(document);

        assertThatThrownBy(() -> service.validateSubmissionDocuments(access, "PRJSUB-001", List.of("DOC-1"), actor))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(exception -> ((BusinessRuleException) exception).code())
                .isEqualTo("DOCUMENT_CONTEXT_MISMATCH");
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

    private DocumentView documentView(
            String id,
            String tenantId,
            DocumentContextType contextType,
            String contextId,
            DocumentStatus status) {
        Instant now = Instant.parse("2026-04-11T12:00:00Z");
        return new DocumentView(
                id,
                tenantId,
                contextType,
                contextId,
                "tenant-a",
                "evidence.pdf",
                "evidence.pdf",
                "application/pdf",
                "pdf",
                1024L,
                "checksum",
                status,
                "USR-1",
                "tenant-a",
                now,
                now,
                null);
    }
}
