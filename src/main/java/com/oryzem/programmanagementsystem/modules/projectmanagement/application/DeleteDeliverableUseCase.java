package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.port.DocumentBindingRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteDeliverableUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final DeliverableSubmissionRepository submissionRepository;
    private final DocumentBindingRepository documentBindingRepository;
    private final ProjectDeliverableRepository deliverableRepository;

    public DeleteDeliverableUseCase(
            ProjectAuthorizationService authorizationService,
            DeliverableSubmissionRepository submissionRepository,
            DocumentBindingRepository documentBindingRepository,
            ProjectDeliverableRepository deliverableRepository) {
        this.authorizationService = authorizationService;
        this.submissionRepository = submissionRepository;
        this.documentBindingRepository = documentBindingRepository;
        this.deliverableRepository = deliverableRepository;
    }

    @Transactional
    public void execute(String projectId, String deliverableId, AuthenticatedUser actor) {
        ProjectAuthorizationService.DeliverableAccess access = authorizationService.authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.EDIT_DELIVERABLE);
        if (submissionRepository.existsByDeliverableId(deliverableId)) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_HAS_SUBMISSIONS",
                    "Project deliverable cannot be removed after submissions exist. Use WAIVED when the deliverable no longer applies.",
                    Map.of("deliverableId", deliverableId));
        }
        if (documentBindingRepository.countTrackedByContext(DocumentContextType.PROJECT_DELIVERABLE, deliverableId) > 0) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_HAS_DOCUMENTS",
                    "Project deliverable cannot be removed while active documents are attached.",
                    Map.of("deliverableId", deliverableId));
        }
        deliverableRepository.deleteById(access.deliverable().id());
    }
}
