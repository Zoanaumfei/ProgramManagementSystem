package com.oryzem.programmanagementsystem.modules.projectmanagement.application.query;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.*;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectMilestonesQuery {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectMilestoneAccessPolicy milestoneAccessPolicy;
    private final ProjectViewMapper viewMapper;

    public ListProjectMilestonesQuery(
            ProjectAuthorizationService authorizationService,
            ProjectMilestoneRepository milestoneRepository,
            ProjectMilestoneAccessPolicy milestoneAccessPolicy,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.milestoneRepository = milestoneRepository;
        this.milestoneAccessPolicy = milestoneAccessPolicy;
        this.viewMapper = viewMapper;
    }

    public List<ProjectViews.ProjectMilestoneView> execute(String projectId, String structureNodeId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access =
                authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_MILESTONE);
        if (structureNodeId != null && !structureNodeId.isBlank()) {
            authorizationService.authorizeStructureNode(projectId, structureNodeId, actor, ProjectPermission.VIEW_PROJECT);
        }
        List<ProjectMilestoneAggregate> milestones =
                structureNodeId != null && !structureNodeId.isBlank()
                        ? milestoneRepository.findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(projectId, structureNodeId)
                        : milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc(projectId);
        return milestones.stream()
                .filter(milestone -> authorizationService.canAccessMilestone(
                        access.project(),
                        access.organizations(),
                        access.members(),
                        milestone,
                        actor,
                        ProjectPermission.VIEW_MILESTONE))
                .map(viewMapper::toMilestoneView)
                .toList();
    }
}


