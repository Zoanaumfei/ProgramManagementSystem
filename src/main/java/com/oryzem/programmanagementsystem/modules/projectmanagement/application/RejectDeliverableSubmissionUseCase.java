package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectDeliverableSubmissionUseCase {

    private static final String IDEMPOTENT_OPERATION = "REJECT_SUBMISSION";

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final DeliverableSubmissionDocumentRepository submissionDocumentRepository;
    private final ProjectViewMapper viewMapper;
    private final ProjectIdempotencyService idempotencyService;
    private final Clock clock;

    public RejectDeliverableSubmissionUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            DeliverableSubmissionRepository submissionRepository,
            DeliverableSubmissionDocumentRepository submissionDocumentRepository,
            ProjectViewMapper viewMapper,
            ProjectIdempotencyService idempotencyService,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.submissionDocumentRepository = submissionDocumentRepository;
        this.viewMapper = viewMapper;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public SubmissionViews.DeliverableSubmissionView execute(String projectId, String deliverableId, String submissionId, ReviewSubmissionCommand command, AuthenticatedUser actor, String idempotencyKey) {
        ProjectAuthorizationService.SubmissionAccess access = authorizationService.authorizeSubmission(projectId, deliverableId, submissionId, actor, ProjectPermission.REJECT_SUBMISSION);
        return idempotencyService.execute(access.project().tenantId(), IDEMPOTENT_OPERATION, idempotencyKey, command, SubmissionViews.DeliverableSubmissionView.class, () -> doReject(access, command, actor));
    }

    private SubmissionViews.DeliverableSubmissionView doReject(ProjectAuthorizationService.SubmissionAccess access, ReviewSubmissionCommand command, AuthenticatedUser actor) {
        if (command.version() != access.submission().version()) {
            throw new ConflictException("Deliverable submission version mismatch.");
        }
        Instant now = Instant.now(clock);
        var savedSubmission = submissionRepository.save(access.submission().reject(actor.userId(), command.reviewComment(), now));
        deliverableRepository.save(access.deliverable().markRejected());
        return viewMapper.toSubmissionView(savedSubmission, submissionDocumentRepository.findAllBySubmissionId(savedSubmission.id()));
    }

    public record ReviewSubmissionCommand(String reviewComment, long version) {
    }
}

