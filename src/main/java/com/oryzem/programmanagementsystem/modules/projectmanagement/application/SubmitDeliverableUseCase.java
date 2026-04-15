package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionDocumentAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitDeliverableUseCase {

    private static final String IDEMPOTENT_OPERATION = "SUBMIT_DELIVERABLE";

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final DeliverableSubmissionDocumentRepository submissionDocumentRepository;
    private final ProjectViewMapper viewMapper;
    private final ProjectIdempotencyService idempotencyService;
    private final ProjectDocumentValidationService documentValidationService;
    private final Clock clock;

    public SubmitDeliverableUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            DeliverableSubmissionRepository submissionRepository,
            DeliverableSubmissionDocumentRepository submissionDocumentRepository,
            ProjectViewMapper viewMapper,
            ProjectIdempotencyService idempotencyService,
            ProjectDocumentValidationService documentValidationService,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.submissionDocumentRepository = submissionDocumentRepository;
        this.viewMapper = viewMapper;
        this.idempotencyService = idempotencyService;
        this.documentValidationService = documentValidationService;
        this.clock = clock;
    }

    @Transactional
    public SubmissionViews.DeliverableSubmissionView execute(String projectId, String deliverableId, SubmitDeliverableCommand command, AuthenticatedUser actor, String idempotencyKey) {
        ProjectAuthorizationService.DeliverableAccess access = authorizationService.authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.SUBMIT_DELIVERABLE);
        return idempotencyService.execute(access.project().tenantId(), IDEMPOTENT_OPERATION, idempotencyKey, command, SubmissionViews.DeliverableSubmissionView.class, () -> doSubmit(access, command, actor));
    }

    private SubmissionViews.DeliverableSubmissionView doSubmit(ProjectAuthorizationService.DeliverableAccess access, SubmitDeliverableCommand command, AuthenticatedUser actor) {
        ProjectDeliverableAggregate deliverable = access.deliverable();
        if (command.deliverableVersion() != deliverable.version()) {
            throw new ConflictException("Project deliverable version mismatch.");
        }
        if (submissionRepository.existsByDeliverableIdAndStatusIn(deliverable.id(), Set.of(DeliverableSubmissionStatus.SUBMITTED, DeliverableSubmissionStatus.UNDER_REVIEW))) {
            throw new BusinessRuleException("DELIVERABLE_SUBMISSION_ALREADY_OPEN", "There is already an open submission for this deliverable.");
        }
        DeliverableSubmissionAggregate latestSubmission = submissionRepository.findTopByDeliverableIdOrderBySubmissionNumberDesc(deliverable.id()).orElse(null);
        int nextSubmissionNumber = latestSubmission != null ? latestSubmission.submissionNumber() + 1 : 1;
        Instant now = Instant.now(clock);
        DeliverableSubmissionAggregate submission = submissionRepository.save(new DeliverableSubmissionAggregate(
                ProjectIds.newSubmissionId(),
                deliverable.id(),
                nextSubmissionNumber,
                actor.userId(),
                actor.organizationId(),
                now,
                DeliverableSubmissionStatus.SUBMITTED,
                null,
                null,
                null,
                0L));
        List<String> validatedDocumentIds = documentValidationService.validateSubmissionDocuments(
                access,
                submission.id(),
                command.documentIds(),
                actor);
        for (String documentId : validatedDocumentIds) {
                submissionDocumentRepository.save(new DeliverableSubmissionDocumentAggregate(
                        ProjectIds.newSubmissionDocumentId(),
                        submission.id(),
                        documentId));
        }
        deliverableRepository.save(deliverable.markSubmitted(now));
        return viewMapper.toSubmissionView(submission, submissionDocumentRepository.findAllBySubmissionId(submission.id()));
    }

    public record SubmitDeliverableCommand(long deliverableVersion, List<String> documentIds) {
    }
}

