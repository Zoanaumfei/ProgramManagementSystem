package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.SubmissionReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionDocumentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListDeliverableSubmissionsUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final DeliverableSubmissionRepository submissionRepository;
    private final DeliverableSubmissionDocumentRepository submissionDocumentRepository;
    private final ProjectViewMapper viewMapper;

    public ListDeliverableSubmissionsUseCase(
            ProjectAuthorizationService authorizationService,
            DeliverableSubmissionRepository submissionRepository,
            DeliverableSubmissionDocumentRepository submissionDocumentRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.submissionRepository = submissionRepository;
        this.submissionDocumentRepository = submissionDocumentRepository;
        this.viewMapper = viewMapper;
    }

    public List<SubmissionReadModels.DeliverableSubmissionReadModel> execute(String projectId, String deliverableId, AuthenticatedUser actor) {
        authorizationService.authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.VIEW_DELIVERABLE);
        return submissionRepository.findAllByDeliverableIdOrderBySubmissionNumberDesc(deliverableId).stream()
                .map(submission -> viewMapper.toSubmissionReadModel(submission, submissionDocumentRepository.findAllBySubmissionId(submission.id())))
                .toList();
    }
}


