package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateMilestoneUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateMilestoneUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.DeleteMilestoneUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListProjectMilestonesQuery;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/milestones")
public class ProjectMilestoneController {

    private final ProjectApiSupport apiSupport;
    private final ListProjectMilestonesQuery listProjectMilestonesQuery;
    private final CreateMilestoneUseCase createMilestoneUseCase;
    private final UpdateMilestoneUseCase updateMilestoneUseCase;
    private final DeleteMilestoneUseCase deleteMilestoneUseCase;

    public ProjectMilestoneController(
            ProjectApiSupport apiSupport,
            ListProjectMilestonesQuery listProjectMilestonesQuery,
            CreateMilestoneUseCase createMilestoneUseCase,
            UpdateMilestoneUseCase updateMilestoneUseCase,
            DeleteMilestoneUseCase deleteMilestoneUseCase) {
        this.apiSupport = apiSupport;
        this.listProjectMilestonesQuery = listProjectMilestonesQuery;
        this.createMilestoneUseCase = createMilestoneUseCase;
        this.updateMilestoneUseCase = updateMilestoneUseCase;
        this.deleteMilestoneUseCase = deleteMilestoneUseCase;
    }

    @GetMapping
    public List<ProjectArtifactDtos.ProjectMilestoneResponse> listMilestones(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return listProjectMilestonesQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)).stream().map(ProjectArtifactDtos.ProjectMilestoneResponse::from).toList();
    }

    @PostMapping
    public ProjectArtifactDtos.ProjectMilestoneResponse createMilestone(Authentication authentication, @PathVariable String projectId, @RequestBody ProjectArtifactDtos.CreateMilestoneRequest request) {
        return ProjectArtifactDtos.ProjectMilestoneResponse.from(createMilestoneUseCase.execute(projectId, new CreateMilestoneUseCase.CreateMilestoneCommand(request.structureNodeId(), request.phaseId(), request.code(), request.name(), request.plannedDate(), request.ownerOrganizationId(), request.visibilityScope()), apiSupport.actor(authentication)));
    }

    @PatchMapping("/{milestoneId}")
    public ProjectArtifactDtos.ProjectMilestoneResponse updateMilestone(Authentication authentication, @PathVariable String projectId, @PathVariable String milestoneId, @RequestBody ProjectArtifactDtos.UpdateMilestoneRequest request) {
        return ProjectArtifactDtos.ProjectMilestoneResponse.from(updateMilestoneUseCase.execute(projectId, milestoneId, new UpdateMilestoneUseCase.UpdateMilestoneCommand(request.code(), request.name(), request.plannedDate(), request.actualDate(), request.status(), request.ownerOrganizationId(), request.visibilityScope(), request.version()), apiSupport.actor(authentication)));
    }

    @DeleteMapping("/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(Authentication authentication, @PathVariable String projectId, @PathVariable String milestoneId) {
        deleteMilestoneUseCase.execute(projectId, milestoneId, apiSupport.actor(authentication));
        return ResponseEntity.noContent().build();
    }
}

