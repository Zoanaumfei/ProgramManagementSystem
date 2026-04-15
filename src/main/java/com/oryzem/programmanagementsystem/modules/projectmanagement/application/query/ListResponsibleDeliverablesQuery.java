package com.oryzem.programmanagementsystem.modules.projectmanagement.application.query;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.*;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListResponsibleDeliverablesQuery {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectVisibilityPolicy visibilityPolicy;
    private final ProjectViewMapper viewMapper;

    public ListResponsibleDeliverablesQuery(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            ProjectVisibilityPolicy visibilityPolicy,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.visibilityPolicy = visibilityPolicy;
        this.viewMapper = viewMapper;
    }

    public List<ProjectViews.ProjectDeliverableView> execute(String projectId, String structureNodeId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_DELIVERABLE);
        if (structureNodeId != null && !structureNodeId.isBlank()) {
            authorizationService.authorizeStructureNode(projectId, structureNodeId, actor, ProjectPermission.VIEW_PROJECT);
        }
        List<ProjectDeliverableAggregate> deliverables =
                structureNodeId != null && !structureNodeId.isBlank()
                        ? deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(projectId, structureNodeId)
                        : deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId);
        return deliverables.stream()
                .filter(deliverable -> visibilityPolicy.matchesAssignment(actor, deliverable.responsibleOrganizationId(), deliverable.responsibleUserId()))
                .filter(deliverable -> authorizationService.canAccessDeliverable(access.project(), access.organizations(), access.members(), deliverable, actor, ProjectPermission.VIEW_DELIVERABLE))
                .map(viewMapper::toDeliverableView)
                .toList();
    }
}


