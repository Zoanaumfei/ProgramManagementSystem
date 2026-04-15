package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetProjectDetailUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListProjectSummariesQuery;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectApiSupport apiSupport;
    private final CreateProjectUseCase createProjectUseCase;
    private final ListProjectSummariesQuery listProjectSummariesQuery;
    private final GetProjectDetailUseCase getProjectDetailUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;

    public ProjectController(
            ProjectApiSupport apiSupport,
            CreateProjectUseCase createProjectUseCase,
            ListProjectSummariesQuery listProjectSummariesQuery,
            GetProjectDetailUseCase getProjectDetailUseCase,
            UpdateProjectUseCase updateProjectUseCase) {
        this.apiSupport = apiSupport;
        this.createProjectUseCase = createProjectUseCase;
        this.listProjectSummariesQuery = listProjectSummariesQuery;
        this.getProjectDetailUseCase = getProjectDetailUseCase;
        this.updateProjectUseCase = updateProjectUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectDtos.ProjectDetailResponse> createProject(
            Authentication authentication,
            @Valid @RequestBody ProjectDtos.CreateProjectRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        ProjectDtos.ProjectDetailResponse response = ProjectDtos.ProjectDetailResponse.from(createProjectUseCase.execute(
                new CreateProjectUseCase.CreateProjectCommand(request.code(), request.name(), request.description(), request.frameworkType(), request.templateId(), request.customerOrganizationId(), request.status(), request.visibilityScope(), request.plannedStartDate(), request.plannedEndDate()),
                apiSupport.actor(authentication),
                idempotencyKey));
        return ResponseEntity.created(URI.create("/api/projects/" + response.id())).body(response);
    }

    @GetMapping
    public List<ProjectDtos.ProjectSummaryResponse> listProjects(Authentication authentication) {
        return listProjectSummariesQuery.execute(apiSupport.actor(authentication)).stream().map(ProjectDtos.ProjectSummaryResponse::from).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectDtos.ProjectDetailResponse getProject(Authentication authentication, @PathVariable String projectId) {
        return ProjectDtos.ProjectDetailResponse.from(getProjectDetailUseCase.execute(projectId, apiSupport.actor(authentication)));
    }

    @PatchMapping("/{projectId}")
    public ProjectDtos.ProjectDetailResponse updateProject(Authentication authentication, @PathVariable String projectId, @Valid @RequestBody ProjectDtos.UpdateProjectRequest request) {
        return ProjectDtos.ProjectDetailResponse.from(updateProjectUseCase.execute(projectId, new UpdateProjectUseCase.UpdateProjectCommand(request.name(), request.description(), request.visibilityScope(), request.plannedStartDate(), request.plannedEndDate(), request.status(), request.version()), apiSupport.actor(authentication)));
    }
}

