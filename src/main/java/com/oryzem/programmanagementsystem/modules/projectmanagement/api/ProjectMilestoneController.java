package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateMilestoneUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListProjectMilestonesQuery;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/milestones")
public class ProjectMilestoneController {

    private final ProjectApiSupport apiSupport;
    private final ListProjectMilestonesQuery listProjectMilestonesQuery;
    private final UpdateMilestoneUseCase updateMilestoneUseCase;

    public ProjectMilestoneController(
            ProjectApiSupport apiSupport,
            ListProjectMilestonesQuery listProjectMilestonesQuery,
            UpdateMilestoneUseCase updateMilestoneUseCase) {
        this.apiSupport = apiSupport;
        this.listProjectMilestonesQuery = listProjectMilestonesQuery;
        this.updateMilestoneUseCase = updateMilestoneUseCase;
    }

    @GetMapping
    public List<ProjectArtifactDtos.ProjectMilestoneResponse> listMilestones(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return listProjectMilestonesQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)).stream().map(ProjectArtifactDtos.ProjectMilestoneResponse::from).toList();
    }

    @PatchMapping("/{milestoneId}")
    public ProjectArtifactDtos.ProjectMilestoneResponse updateMilestone(Authentication authentication, @PathVariable String projectId, @PathVariable String milestoneId, @RequestBody ProjectArtifactDtos.UpdateMilestoneRequest request) {
        return ProjectArtifactDtos.ProjectMilestoneResponse.from(updateMilestoneUseCase.execute(projectId, milestoneId, new UpdateMilestoneUseCase.UpdateMilestoneCommand(request.plannedDate(), request.actualDate(), request.status(), request.ownerOrganizationId(), request.visibilityScope(), request.version()), apiSupport.actor(authentication)));
    }
}

