package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.SubmissionReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetDeliverableSubmissionDetailUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final DeliverableSubmissionDocumentRepository submissionDocumentRepository;
    private final ProjectViewMapper viewMapper;

    public GetDeliverableSubmissionDetailUseCase(
            ProjectAuthorizationService authorizationService,
            DeliverableSubmissionDocumentRepository submissionDocumentRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.submissionDocumentRepository = submissionDocumentRepository;
        this.viewMapper = viewMapper;
    }

    public SubmissionReadModels.DeliverableSubmissionReadModel execute(String projectId, String deliverableId, String submissionId, AuthenticatedUser actor) {
        ProjectAuthorizationService.SubmissionAccess access = authorizationService.authorizeSubmission(projectId, deliverableId, submissionId, actor, ProjectPermission.VIEW_DELIVERABLE);
        return viewMapper.toSubmissionReadModel(access.submission(), submissionDocumentRepository.findAllBySubmissionId(submissionId));
    }
}


