package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectFrameworkUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectFrameworksUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectFrameworkUseCase;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-frameworks")
public class ProjectFrameworkController {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final ListProjectFrameworksUseCase listProjectFrameworksUseCase;
    private final CreateProjectFrameworkUseCase createProjectFrameworkUseCase;
    private final UpdateProjectFrameworkUseCase updateProjectFrameworkUseCase;

    public ProjectFrameworkController(
            AuthenticatedUserMapper authenticatedUserMapper,
            ListProjectFrameworksUseCase listProjectFrameworksUseCase,
            CreateProjectFrameworkUseCase createProjectFrameworkUseCase,
            UpdateProjectFrameworkUseCase updateProjectFrameworkUseCase) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.listProjectFrameworksUseCase = listProjectFrameworksUseCase;
        this.createProjectFrameworkUseCase = createProjectFrameworkUseCase;
        this.updateProjectFrameworkUseCase = updateProjectFrameworkUseCase;
    }

    @GetMapping
    public List<ProjectFrameworkDtos.ProjectFrameworkResponse> listFrameworks(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listProjectFrameworksUseCase.execute(actor).stream()
                .map(ProjectFrameworkDtos.ProjectFrameworkResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProjectFrameworkDtos.ProjectFrameworkResponse> createFramework(
            Authentication authentication,
            @Valid @RequestBody ProjectFrameworkDtos.CreateProjectFrameworkRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        ProjectFrameworkDtos.ProjectFrameworkResponse response = ProjectFrameworkDtos.ProjectFrameworkResponse.from(
                createProjectFrameworkUseCase.execute(
                        new CreateProjectFrameworkUseCase.CreateProjectFrameworkCommand(
                                request.code(),
                                request.displayName(),
                                request.description(),
                                request.uiLayout(),
                                request.active()),
                        actor));
        return ResponseEntity.created(URI.create("/api/project-frameworks/" + response.id())).body(response);
    }

    @PatchMapping("/{frameworkId}")
    public ProjectFrameworkDtos.ProjectFrameworkResponse updateFramework(
            Authentication authentication,
            @PathVariable String frameworkId,
            @Valid @RequestBody ProjectFrameworkDtos.UpdateProjectFrameworkRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectFrameworkDtos.ProjectFrameworkResponse.from(
                updateProjectFrameworkUseCase.execute(
                        frameworkId,
                        new UpdateProjectFrameworkUseCase.UpdateProjectFrameworkCommand(
                                request.displayName(),
                                request.description(),
                                request.uiLayout(),
                                request.active()),
                        actor));
    }
}
