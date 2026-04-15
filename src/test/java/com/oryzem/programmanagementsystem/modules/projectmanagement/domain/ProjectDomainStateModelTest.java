package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProjectDomainStateModelTest {

    @Test
    void shouldAllowProjectStatusTransitionFromDraftToPlanned() {
        ProjectAggregate aggregate = project();

        ProjectAggregate transitioned = aggregate.transitionTo(ProjectStatus.PLANNED, Instant.now(), LocalDate.now());

        assertThat(transitioned.status()).isEqualTo(ProjectStatus.PLANNED);
    }

    @Test
    void shouldBlockInvalidProjectStatusTransition() {
        ProjectAggregate aggregate = project().transitionTo(ProjectStatus.PLANNED, Instant.now(), LocalDate.now());

        assertThatThrownBy(() -> aggregate.transitionTo(ProjectStatus.COMPLETED, Instant.now(), LocalDate.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("transition");
    }

    @Test
    void shouldRequireResponsibleOrganizationBeforeSubmission() {
        ProjectDeliverableAggregate aggregate = deliverable(null, null);

        assertThatThrownBy(() -> aggregate.markSubmitted(Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("responsible organization");
    }

    @Test
    void shouldBlockSubmissionWhenDeliverableAlreadyApproved() {
        ProjectDeliverableAggregate aggregate = deliverable("tenant-a", null).markSubmitted(Instant.now()).markApproved(Instant.now());

        assertThatThrownBy(() -> aggregate.markSubmitted(Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot receive a new submission");
    }

    @Test
    void shouldApproveSubmittedDeliverable() {
        ProjectDeliverableAggregate aggregate = deliverable("tenant-a", null).markSubmitted(Instant.now());

        ProjectDeliverableAggregate approved = aggregate.markApproved(Instant.now());

        assertThat(approved.status()).isEqualTo(ProjectDeliverableStatus.APPROVED);
        assertThat(approved.approvedAt()).isNotNull();
    }

    @Test
    void shouldRejectApprovedSubmissionTransition() {
        DeliverableSubmissionAggregate aggregate = submission().approve("USR-1", "ok", Instant.now());

        assertThatThrownBy(() -> aggregate.reject("USR-2", "retry", Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rejected");
    }

    @Test
    void shouldBlockOperationalEditWhenDeliverableIsUnderReview() {
        ProjectDeliverableAggregate aggregate = new ProjectDeliverableAggregate(
                "DLV-1",
                "PRJ-1",
                "PRJ-1-ROOT",
                null,
                null,
                "CODE",
                "Deliverable",
                "desc",
                DeliverableType.DOCUMENT_PACKAGE,
                "tenant-a",
                null,
                null,
                null,
                true,
                LocalDate.now().plusDays(5),
                Instant.now(),
                null,
                ProjectDeliverableStatus.UNDER_REVIEW,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER,
                0L);

        assertThatThrownBy(() -> aggregate.updateOperationalFields(
                        "new desc",
                        "tenant-a",
                        null,
                        null,
                        null,
                        LocalDate.now().plusDays(7),
                        ProjectDeliverableStatus.UNDER_REVIEW,
                        ProjectPriority.HIGH,
                        ProjectVisibilityScope.LEAD_ONLY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("submission workflow");
    }

    private ProjectAggregate project() {
        return new ProjectAggregate(
                "PRJ-1",
                "tenant-a",
                "PRJ-CODE",
                "Project",
                "desc",
                ProjectFrameworkType.APQP,
                "TMP-1",
                1,
                "tenant-a",
                null,
                ProjectStatus.DRAFT,
                ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                null,
                "USR-1",
                Instant.now(),
                Instant.now(),
                0L);
    }

    private ProjectDeliverableAggregate deliverable(String responsibleOrganizationId, String approverOrganizationId) {
        return new ProjectDeliverableAggregate(
                "DLV-1",
                "PRJ-1",
                "PRJ-1-ROOT",
                null,
                null,
                "CODE",
                "Deliverable",
                "desc",
                DeliverableType.DOCUMENT_PACKAGE,
                responsibleOrganizationId,
                null,
                approverOrganizationId,
                null,
                true,
                LocalDate.now().plusDays(5),
                null,
                null,
                ProjectDeliverableStatus.READY_FOR_SUBMISSION,
                ProjectPriority.HIGH,
                ProjectVisibilityScope.RESPONSIBLE_AND_APPROVER,
                0L);
    }

    private DeliverableSubmissionAggregate submission() {
        return new DeliverableSubmissionAggregate(
                "SUB-1",
                "DLV-1",
                1,
                "USR-1",
                "tenant-a",
                Instant.now(),
                DeliverableSubmissionStatus.SUBMITTED,
                null,
                null,
                null,
                0L);
    }
}
