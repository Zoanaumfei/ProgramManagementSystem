package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMilestoneUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectMilestoneRepository milestoneRepository;

    public DeleteMilestoneUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            ProjectMilestoneRepository milestoneRepository) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public void execute(String projectId, String milestoneId, AuthenticatedUser actor) {
        ProjectAuthorizationService.MilestoneAccess access = authorizationService.authorizeMilestone(projectId, milestoneId, actor, ProjectPermission.EDIT_MILESTONE);
        boolean hasLinkedDeliverables = deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId).stream()
                .anyMatch(deliverable -> milestoneId.equals(deliverable.milestoneId()));
        if (hasLinkedDeliverables) {
            throw new BusinessRuleException(
                    "PROJECT_MILESTONE_IN_USE",
                    "Project milestone cannot be removed while deliverables reference it.",
                    Map.of("milestoneId", milestoneId));
        }
        milestoneRepository.deleteById(access.milestone().id());
    }
}
