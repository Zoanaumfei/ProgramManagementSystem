package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.DashboardReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetProjectDashboardUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectDeliverableAccessPolicy deliverableAccessPolicy;
    private final ProjectMilestoneAccessPolicy milestoneAccessPolicy;
    private final Clock clock;

    public GetProjectDashboardUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectDeliverableAccessPolicy deliverableAccessPolicy,
            ProjectMilestoneAccessPolicy milestoneAccessPolicy,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.milestoneRepository = milestoneRepository;
        this.deliverableAccessPolicy = deliverableAccessPolicy;
        this.milestoneAccessPolicy = milestoneAccessPolicy;
        this.clock = clock;
    }

    public DashboardReadModels.ProjectDashboardReadModel execute(String projectId, String structureNodeId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access =
                authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_PROJECT);
        if (structureNodeId != null && !structureNodeId.isBlank()) {
            authorizationService.authorizeStructureNode(projectId, structureNodeId, actor, ProjectPermission.VIEW_PROJECT);
        }
        List<ProjectDeliverableAggregate> deliverables = structureNodeId != null && !structureNodeId.isBlank()
                ? deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(projectId, structureNodeId)
                : deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId);
        List<ProjectMilestoneAggregate> milestones = structureNodeId != null && !structureNodeId.isBlank()
                ? milestoneRepository.findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(projectId, structureNodeId)
                : milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc(projectId);
        List<ProjectDeliverableAggregate> visibleDeliverables = deliverables.stream()
                .filter(deliverable -> authorizationService.canAccessDeliverable(
                        access.project(),
                        access.organizations(),
                        access.members(),
                        deliverable,
                        actor,
                        ProjectPermission.VIEW_DELIVERABLE))
                .toList();
        List<ProjectMilestoneAggregate> visibleMilestones = milestones.stream()
                .filter(milestone -> authorizationService.canAccessMilestone(
                        access.project(),
                        access.organizations(),
                        access.members(),
                        milestone,
                        actor,
                        ProjectPermission.VIEW_MILESTONE))
                .toList();
        LocalDate today = LocalDate.now(clock);
        return new DashboardReadModels.ProjectDashboardReadModel(
                projectId,
                visibleDeliverables.size(),
                visibleDeliverables.stream().filter(deliverable -> deliverable.status() == ProjectDeliverableStatus.READY_FOR_SUBMISSION).count(),
                visibleDeliverables.stream().filter(deliverable -> deliverable.status() == ProjectDeliverableStatus.SUBMITTED || deliverable.status() == ProjectDeliverableStatus.UNDER_REVIEW).count(),
                visibleDeliverables.stream().filter(deliverable -> deliverable.status() == ProjectDeliverableStatus.APPROVED).count(),
                visibleDeliverables.stream().filter(deliverable -> deliverable.status() == ProjectDeliverableStatus.REJECTED).count(),
                visibleDeliverables.stream().filter(deliverable -> deliverable.plannedDueDate() != null && deliverable.plannedDueDate().isBefore(today) && deliverable.status() != ProjectDeliverableStatus.APPROVED).count(),
                visibleMilestones.stream().filter(milestone -> milestone.status() == ProjectMilestoneStatus.AT_RISK || milestone.status() == ProjectMilestoneStatus.DELAYED).count(),
                visibleMilestones.stream().map(ProjectMilestoneAggregate::plannedDate).filter(java.util.Objects::nonNull).filter(date -> !date.isBefore(today)).min(Comparator.naturalOrder()).orElse(null));
    }
}



