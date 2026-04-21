package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectPurgeIntentUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.PurgeProjectUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/projects")
public class ProjectAdministrationController {

    private final ProjectApiSupport apiSupport;
    private final CreateProjectPurgeIntentUseCase createProjectPurgeIntentUseCase;
    private final PurgeProjectUseCase purgeProjectUseCase;

    public ProjectAdministrationController(
            ProjectApiSupport apiSupport,
            CreateProjectPurgeIntentUseCase createProjectPurgeIntentUseCase,
            PurgeProjectUseCase purgeProjectUseCase) {
        this.apiSupport = apiSupport;
        this.createProjectPurgeIntentUseCase = createProjectPurgeIntentUseCase;
        this.purgeProjectUseCase = purgeProjectUseCase;
    }

    @PostMapping("/{projectId}/purge-intents")
    public ProjectPurgeDtos.ProjectPurgeIntentResponse createPurgeIntent(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectPurgeDtos.CreateProjectPurgeIntentRequest request) {
        return ProjectPurgeDtos.ProjectPurgeIntentResponse.from(
                createProjectPurgeIntentUseCase.execute(projectId, request.reason(), apiSupport.actor(authentication)));
    }

    @PostMapping("/{projectId}/purge")
    public ProjectPurgeDtos.ProjectPurgeExecutionResponse purgeProject(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectPurgeDtos.ExecuteProjectPurgeRequest request) {
        return ProjectPurgeDtos.ProjectPurgeExecutionResponse.from(
                purgeProjectUseCase.execute(
                        projectId,
                        new PurgeProjectUseCase.ExecuteProjectPurgeCommand(
                                request.reason(),
                                request.purgeToken(),
                                request.confirm(),
                                request.confirmationText()),
                        apiSupport.actor(authentication)));
    }
}
