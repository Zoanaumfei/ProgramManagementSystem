package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectStructureNodeUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetProjectStructureTreeUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.MoveProjectStructureNodeUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectStructureNodeUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/structure")
public class ProjectStructureController {

    private final ProjectApiSupport apiSupport;
    private final GetProjectStructureTreeUseCase getProjectStructureTreeUseCase;
    private final CreateProjectStructureNodeUseCase createProjectStructureNodeUseCase;
    private final UpdateProjectStructureNodeUseCase updateProjectStructureNodeUseCase;
    private final MoveProjectStructureNodeUseCase moveProjectStructureNodeUseCase;

    public ProjectStructureController(
            ProjectApiSupport apiSupport,
            GetProjectStructureTreeUseCase getProjectStructureTreeUseCase,
            CreateProjectStructureNodeUseCase createProjectStructureNodeUseCase,
            UpdateProjectStructureNodeUseCase updateProjectStructureNodeUseCase,
            MoveProjectStructureNodeUseCase moveProjectStructureNodeUseCase) {
        this.apiSupport = apiSupport;
        this.getProjectStructureTreeUseCase = getProjectStructureTreeUseCase;
        this.createProjectStructureNodeUseCase = createProjectStructureNodeUseCase;
        this.updateProjectStructureNodeUseCase = updateProjectStructureNodeUseCase;
        this.moveProjectStructureNodeUseCase = moveProjectStructureNodeUseCase;
    }

    @GetMapping
    public ProjectStructureDtos.ProjectStructureTreeResponse getStructure(Authentication authentication, @PathVariable String projectId) {
        return ProjectStructureDtos.ProjectStructureTreeResponse.from(getProjectStructureTreeUseCase.execute(projectId, apiSupport.actor(authentication)));
    }

    @PostMapping("/nodes")
    public ProjectStructureDtos.ProjectStructureNodeResponse createStructureNode(Authentication authentication, @PathVariable String projectId, @Valid @RequestBody ProjectStructureDtos.CreateProjectStructureNodeRequest request) {
        return ProjectStructureDtos.ProjectStructureNodeResponse.from(createProjectStructureNodeUseCase.execute(projectId, new CreateProjectStructureNodeUseCase.CreateProjectStructureNodeCommand(request.parentNodeId(), request.name(), request.code(), request.ownerOrganizationId(), request.responsibleUserId(), request.visibilityScope()), apiSupport.actor(authentication)));
    }

    @PatchMapping("/nodes/{nodeId}")
    public ProjectStructureDtos.ProjectStructureNodeResponse updateStructureNode(Authentication authentication, @PathVariable String projectId, @PathVariable String nodeId, @Valid @RequestBody ProjectStructureDtos.UpdateProjectStructureNodeRequest request) {
        return ProjectStructureDtos.ProjectStructureNodeResponse.from(updateProjectStructureNodeUseCase.execute(projectId, nodeId, new UpdateProjectStructureNodeUseCase.UpdateProjectStructureNodeCommand(request.name(), request.code(), request.ownerOrganizationId(), request.responsibleUserId(), request.visibilityScope(), request.version()), apiSupport.actor(authentication)));
    }

    @PostMapping("/nodes/{nodeId}/move")
    public ProjectStructureDtos.ProjectStructureNodeResponse moveStructureNode(Authentication authentication, @PathVariable String projectId, @PathVariable String nodeId, @Valid @RequestBody ProjectStructureDtos.MoveProjectStructureNodeRequest request) {
        return ProjectStructureDtos.ProjectStructureNodeResponse.from(moveProjectStructureNodeUseCase.execute(projectId, nodeId, new MoveProjectStructureNodeUseCase.MoveProjectStructureNodeCommand(request.newParentNodeId(), request.version()), apiSupport.actor(authentication)));
    }
}

