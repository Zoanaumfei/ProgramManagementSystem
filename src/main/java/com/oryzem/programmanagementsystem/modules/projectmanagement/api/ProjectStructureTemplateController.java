package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectStructureLevelTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectStructureTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetProjectStructureTemplateDetailUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectStructureTemplatesUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.PurgeProjectStructureTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ReorderProjectStructureLevelsUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.SetProjectStructureTemplateActivationUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectStructureLevelTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectStructureTemplateUseCase;
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
@RequestMapping("/api/project-structure-templates")
public class ProjectStructureTemplateController {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final CreateProjectStructureTemplateUseCase createProjectStructureTemplateUseCase;
    private final UpdateProjectStructureTemplateUseCase updateProjectStructureTemplateUseCase;
    private final SetProjectStructureTemplateActivationUseCase setProjectStructureTemplateActivationUseCase;
    private final PurgeProjectStructureTemplateUseCase purgeProjectStructureTemplateUseCase;
    private final CreateProjectStructureLevelTemplateUseCase createProjectStructureLevelTemplateUseCase;
    private final UpdateProjectStructureLevelTemplateUseCase updateProjectStructureLevelTemplateUseCase;
    private final ReorderProjectStructureLevelsUseCase reorderProjectStructureLevelsUseCase;
    private final ListProjectStructureTemplatesUseCase listProjectStructureTemplatesUseCase;
    private final GetProjectStructureTemplateDetailUseCase getProjectStructureTemplateDetailUseCase;

    public ProjectStructureTemplateController(
            AuthenticatedUserMapper authenticatedUserMapper,
            CreateProjectStructureTemplateUseCase createProjectStructureTemplateUseCase,
            UpdateProjectStructureTemplateUseCase updateProjectStructureTemplateUseCase,
            SetProjectStructureTemplateActivationUseCase setProjectStructureTemplateActivationUseCase,
            PurgeProjectStructureTemplateUseCase purgeProjectStructureTemplateUseCase,
            CreateProjectStructureLevelTemplateUseCase createProjectStructureLevelTemplateUseCase,
            UpdateProjectStructureLevelTemplateUseCase updateProjectStructureLevelTemplateUseCase,
            ReorderProjectStructureLevelsUseCase reorderProjectStructureLevelsUseCase,
            ListProjectStructureTemplatesUseCase listProjectStructureTemplatesUseCase,
            GetProjectStructureTemplateDetailUseCase getProjectStructureTemplateDetailUseCase) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.createProjectStructureTemplateUseCase = createProjectStructureTemplateUseCase;
        this.updateProjectStructureTemplateUseCase = updateProjectStructureTemplateUseCase;
        this.setProjectStructureTemplateActivationUseCase = setProjectStructureTemplateActivationUseCase;
        this.purgeProjectStructureTemplateUseCase = purgeProjectStructureTemplateUseCase;
        this.createProjectStructureLevelTemplateUseCase = createProjectStructureLevelTemplateUseCase;
        this.updateProjectStructureLevelTemplateUseCase = updateProjectStructureLevelTemplateUseCase;
        this.reorderProjectStructureLevelsUseCase = reorderProjectStructureLevelsUseCase;
        this.listProjectStructureTemplatesUseCase = listProjectStructureTemplatesUseCase;
        this.getProjectStructureTemplateDetailUseCase = getProjectStructureTemplateDetailUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectStructureDtos.ProjectStructureTemplateDetailResponse> createTemplate(
            Authentication authentication,
            @Valid @RequestBody ProjectStructureDtos.CreateProjectStructureTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        ProjectStructureDtos.ProjectStructureTemplateDetailResponse response = ProjectStructureDtos.ProjectStructureTemplateDetailResponse.from(
                createProjectStructureTemplateUseCase.execute(
                        new CreateProjectStructureTemplateUseCase.CreateProjectStructureTemplateCommand(
                                request.name(),
                                request.frameworkType(),
                                request.version(),
                                request.active()),
                        actor));
        return ResponseEntity.created(URI.create("/api/project-structure-templates/" + response.id())).body(response);
    }

    @GetMapping
    public List<ProjectStructureDtos.ProjectStructureTemplateSummaryResponse> listTemplates(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listProjectStructureTemplatesUseCase.execute(actor).stream()
                .map(ProjectStructureDtos.ProjectStructureTemplateSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{structureTemplateId}")
    public ProjectStructureDtos.ProjectStructureTemplateDetailResponse getTemplate(Authentication authentication, @PathVariable String structureTemplateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureTemplateDetailResponse.from(
                getProjectStructureTemplateDetailUseCase.execute(structureTemplateId, actor));
    }

    @PatchMapping("/{structureTemplateId}")
    public ProjectStructureDtos.ProjectStructureTemplateDetailResponse updateTemplate(
            Authentication authentication,
            @PathVariable String structureTemplateId,
            @Valid @RequestBody ProjectStructureDtos.UpdateProjectStructureTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureTemplateDetailResponse.from(
                updateProjectStructureTemplateUseCase.execute(
                        structureTemplateId,
                        new UpdateProjectStructureTemplateUseCase.UpdateProjectStructureTemplateCommand(request.name()),
                        actor));
    }

    @PostMapping("/{structureTemplateId}/purge")
    public ProjectStructureDtos.ProjectStructureTemplateSummaryResponse purgeTemplate(
            Authentication authentication,
            @PathVariable String structureTemplateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureTemplateSummaryResponse.from(
                purgeProjectStructureTemplateUseCase.execute(structureTemplateId, actor));
    }

    @PostMapping("/{structureTemplateId}/activate")
    public ProjectStructureDtos.ProjectStructureTemplateSummaryResponse activateTemplate(Authentication authentication, @PathVariable String structureTemplateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureTemplateSummaryResponse.from(
                setProjectStructureTemplateActivationUseCase.execute(structureTemplateId, true, actor));
    }

    @PostMapping("/{structureTemplateId}/deactivate")
    public ProjectStructureDtos.ProjectStructureTemplateSummaryResponse deactivateTemplate(Authentication authentication, @PathVariable String structureTemplateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureTemplateSummaryResponse.from(
                setProjectStructureTemplateActivationUseCase.execute(structureTemplateId, false, actor));
    }

    @PostMapping("/{structureTemplateId}/levels")
    public ProjectStructureDtos.ProjectStructureLevelResponse createLevel(
            Authentication authentication,
            @PathVariable String structureTemplateId,
            @Valid @RequestBody ProjectStructureDtos.CreateProjectStructureLevelTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureLevelResponse.from(
                createProjectStructureLevelTemplateUseCase.execute(
                        structureTemplateId,
                        new CreateProjectStructureLevelTemplateUseCase.CreateProjectStructureLevelTemplateCommand(
                                request.name(),
                                request.code(),
                                request.allowsChildren(),
                                request.allowsMilestones(),
                                request.allowsDeliverables()),
                        actor));
    }

    @PatchMapping("/{structureTemplateId}/levels/{levelTemplateId}")
    public ProjectStructureDtos.ProjectStructureLevelResponse updateLevel(
            Authentication authentication,
            @PathVariable String structureTemplateId,
            @PathVariable String levelTemplateId,
            @Valid @RequestBody ProjectStructureDtos.UpdateProjectStructureLevelTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectStructureDtos.ProjectStructureLevelResponse.from(
                updateProjectStructureLevelTemplateUseCase.execute(
                        structureTemplateId,
                        levelTemplateId,
                        new UpdateProjectStructureLevelTemplateUseCase.UpdateProjectStructureLevelTemplateCommand(
                                request.name(),
                                request.code(),
                                request.allowsChildren(),
                                request.allowsMilestones(),
                                request.allowsDeliverables()),
                        actor));
    }

    @PostMapping("/{structureTemplateId}/levels/reorder")
    public List<ProjectStructureDtos.ProjectStructureLevelResponse> reorderLevels(
            Authentication authentication,
            @PathVariable String structureTemplateId,
            @Valid @RequestBody ProjectStructureDtos.ReorderProjectStructureLevelsRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return reorderProjectStructureLevelsUseCase.execute(structureTemplateId, request.orderedLevelIds(), actor).stream()
                .map(ProjectStructureDtos.ProjectStructureLevelResponse::from)
                .toList();
    }
}

