package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateDeliverableTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectMilestoneTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateProjectPhaseTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetProjectTemplateDetailUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListDeliverableTemplatesUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectMilestoneTemplatesUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectPhaseTemplatesUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectTemplatesUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateDeliverableTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectMilestoneTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectPhaseTemplateUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateProjectTemplateUseCase;
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
@RequestMapping("/api/project-templates")
public class ProjectTemplateController {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final ListProjectTemplatesUseCase listProjectTemplatesUseCase;
    private final GetProjectTemplateDetailUseCase getProjectTemplateDetailUseCase;
    private final CreateProjectTemplateUseCase createProjectTemplateUseCase;
    private final UpdateProjectTemplateUseCase updateProjectTemplateUseCase;
    private final ListProjectPhaseTemplatesUseCase listProjectPhaseTemplatesUseCase;
    private final CreateProjectPhaseTemplateUseCase createProjectPhaseTemplateUseCase;
    private final UpdateProjectPhaseTemplateUseCase updateProjectPhaseTemplateUseCase;
    private final ListProjectMilestoneTemplatesUseCase listProjectMilestoneTemplatesUseCase;
    private final CreateProjectMilestoneTemplateUseCase createProjectMilestoneTemplateUseCase;
    private final UpdateProjectMilestoneTemplateUseCase updateProjectMilestoneTemplateUseCase;
    private final ListDeliverableTemplatesUseCase listDeliverableTemplatesUseCase;
    private final CreateDeliverableTemplateUseCase createDeliverableTemplateUseCase;
    private final UpdateDeliverableTemplateUseCase updateDeliverableTemplateUseCase;

    public ProjectTemplateController(
            AuthenticatedUserMapper authenticatedUserMapper,
            ListProjectTemplatesUseCase listProjectTemplatesUseCase,
            GetProjectTemplateDetailUseCase getProjectTemplateDetailUseCase,
            CreateProjectTemplateUseCase createProjectTemplateUseCase,
            UpdateProjectTemplateUseCase updateProjectTemplateUseCase,
            ListProjectPhaseTemplatesUseCase listProjectPhaseTemplatesUseCase,
            CreateProjectPhaseTemplateUseCase createProjectPhaseTemplateUseCase,
            UpdateProjectPhaseTemplateUseCase updateProjectPhaseTemplateUseCase,
            ListProjectMilestoneTemplatesUseCase listProjectMilestoneTemplatesUseCase,
            CreateProjectMilestoneTemplateUseCase createProjectMilestoneTemplateUseCase,
            UpdateProjectMilestoneTemplateUseCase updateProjectMilestoneTemplateUseCase,
            ListDeliverableTemplatesUseCase listDeliverableTemplatesUseCase,
            CreateDeliverableTemplateUseCase createDeliverableTemplateUseCase,
            UpdateDeliverableTemplateUseCase updateDeliverableTemplateUseCase) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.listProjectTemplatesUseCase = listProjectTemplatesUseCase;
        this.getProjectTemplateDetailUseCase = getProjectTemplateDetailUseCase;
        this.createProjectTemplateUseCase = createProjectTemplateUseCase;
        this.updateProjectTemplateUseCase = updateProjectTemplateUseCase;
        this.listProjectPhaseTemplatesUseCase = listProjectPhaseTemplatesUseCase;
        this.createProjectPhaseTemplateUseCase = createProjectPhaseTemplateUseCase;
        this.updateProjectPhaseTemplateUseCase = updateProjectPhaseTemplateUseCase;
        this.listProjectMilestoneTemplatesUseCase = listProjectMilestoneTemplatesUseCase;
        this.createProjectMilestoneTemplateUseCase = createProjectMilestoneTemplateUseCase;
        this.updateProjectMilestoneTemplateUseCase = updateProjectMilestoneTemplateUseCase;
        this.listDeliverableTemplatesUseCase = listDeliverableTemplatesUseCase;
        this.createDeliverableTemplateUseCase = createDeliverableTemplateUseCase;
        this.updateDeliverableTemplateUseCase = updateDeliverableTemplateUseCase;
    }

    @GetMapping
    public List<ProjectTemplateDtos.ProjectTemplateSummaryResponse> listTemplates(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listProjectTemplatesUseCase.execute(actor).stream()
                .map(ProjectTemplateDtos.ProjectTemplateSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{templateId}")
    public ProjectTemplateDtos.ProjectTemplateDetailResponse getTemplate(Authentication authentication, @PathVariable String templateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateDetailResponse.from(getProjectTemplateDetailUseCase.execute(templateId, actor));
    }

    @PostMapping
    public ResponseEntity<ProjectTemplateDtos.ProjectTemplateDetailResponse> createTemplate(
            Authentication authentication,
            @Valid @RequestBody ProjectTemplateDtos.CreateProjectTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        ProjectTemplateDtos.ProjectTemplateDetailResponse response = ProjectTemplateDtos.ProjectTemplateDetailResponse.from(
                createProjectTemplateUseCase.execute(
                        new CreateProjectTemplateUseCase.CreateProjectTemplateCommand(
                                request.name(),
                                request.frameworkType(),
                                request.version(),
                                request.status(),
                                request.isDefault(),
                                request.structureTemplateId()),
                        actor));
        return ResponseEntity.created(URI.create("/api/project-templates/" + response.id())).body(response);
    }

    @PatchMapping("/{templateId}")
    public ProjectTemplateDtos.ProjectTemplateDetailResponse updateTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @Valid @RequestBody ProjectTemplateDtos.UpdateProjectTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateDetailResponse.from(
                updateProjectTemplateUseCase.execute(
                        templateId,
                        new UpdateProjectTemplateUseCase.UpdateProjectTemplateCommand(
                                request.name(),
                                request.status(),
                                request.isDefault(),
                                request.structureTemplateId()),
                        actor));
    }

    @GetMapping("/{templateId}/phases")
    public List<ProjectTemplateDtos.ProjectPhaseTemplateResponse> listPhaseTemplates(Authentication authentication, @PathVariable String templateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listProjectPhaseTemplatesUseCase.execute(templateId, actor).stream()
                .map(ProjectTemplateDtos.ProjectPhaseTemplateResponse::from)
                .toList();
    }

    @PostMapping("/{templateId}/phases")
    public ProjectTemplateDtos.ProjectPhaseTemplateResponse createPhaseTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @Valid @RequestBody ProjectTemplateDtos.CreateProjectPhaseTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectPhaseTemplateResponse.from(
                createProjectPhaseTemplateUseCase.execute(
                        templateId,
                        new CreateProjectPhaseTemplateUseCase.CreateProjectPhaseTemplateCommand(
                                request.name(),
                                request.description(),
                                request.plannedStartOffsetDays(),
                                request.plannedEndOffsetDays()),
                        actor));
    }

    @PatchMapping("/{templateId}/phases/{phaseTemplateId}")
    public ProjectTemplateDtos.ProjectPhaseTemplateResponse updatePhaseTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @PathVariable String phaseTemplateId,
            @Valid @RequestBody ProjectTemplateDtos.UpdateProjectPhaseTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectPhaseTemplateResponse.from(
                updateProjectPhaseTemplateUseCase.execute(
                        templateId,
                        phaseTemplateId,
                        new UpdateProjectPhaseTemplateUseCase.UpdateProjectPhaseTemplateCommand(
                                request.name(),
                                request.description(),
                                request.plannedStartOffsetDays(),
                                request.plannedEndOffsetDays()),
                        actor));
    }

    @GetMapping("/{templateId}/milestones")
    public List<ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse> listMilestoneTemplates(Authentication authentication, @PathVariable String templateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listProjectMilestoneTemplatesUseCase.execute(templateId, actor).stream()
                .map(ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse::from)
                .toList();
    }

    @PostMapping("/{templateId}/milestones")
    public ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse createMilestoneTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @Valid @RequestBody ProjectTemplateDtos.CreateProjectMilestoneTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse.from(
                createProjectMilestoneTemplateUseCase.execute(
                        templateId,
                        new CreateProjectMilestoneTemplateUseCase.CreateProjectMilestoneTemplateCommand(
                                request.phaseTemplateId(),
                                request.code(),
                                request.name(),
                                request.description(),
                                request.plannedOffsetDays(),
                                request.appliesToType(),
                                request.structureLevelTemplateId(),
                                request.ownerOrganizationRole(),
                                request.visibilityScope()),
                        actor));
    }

    @PatchMapping("/{templateId}/milestones/{milestoneTemplateId}")
    public ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse updateMilestoneTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @PathVariable String milestoneTemplateId,
            @Valid @RequestBody ProjectTemplateDtos.UpdateProjectMilestoneTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateMilestoneTemplateResponse.from(
                updateProjectMilestoneTemplateUseCase.execute(
                        templateId,
                        milestoneTemplateId,
                        new UpdateProjectMilestoneTemplateUseCase.UpdateProjectMilestoneTemplateCommand(
                                request.phaseTemplateId(),
                                request.code(),
                                request.name(),
                                request.description(),
                                request.plannedOffsetDays(),
                                request.appliesToType(),
                                request.structureLevelTemplateId(),
                                request.ownerOrganizationRole(),
                                request.visibilityScope()),
                        actor));
    }

    @GetMapping("/{templateId}/deliverables")
    public List<ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse> listDeliverableTemplates(Authentication authentication, @PathVariable String templateId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listDeliverableTemplatesUseCase.execute(templateId, actor).stream()
                .map(ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse::from)
                .toList();
    }

    @PostMapping("/{templateId}/deliverables")
    public ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse createDeliverableTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @Valid @RequestBody ProjectTemplateDtos.CreateDeliverableTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse.from(
                createDeliverableTemplateUseCase.execute(
                        templateId,
                        new CreateDeliverableTemplateUseCase.CreateDeliverableTemplateCommand(
                                request.phaseTemplateId(),
                                request.milestoneTemplateId(),
                                request.code(),
                                request.name(),
                                request.description(),
                                request.deliverableType(),
                                request.requiredDocument(),
                                request.plannedDueOffsetDays(),
                                request.appliesToType(),
                                request.structureLevelTemplateId(),
                                request.responsibleOrganizationRole(),
                                request.approverOrganizationRole(),
                                request.visibilityScope(),
                                request.priority()),
                        actor));
    }

    @PatchMapping("/{templateId}/deliverables/{deliverableTemplateId}")
    public ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse updateDeliverableTemplate(
            Authentication authentication,
            @PathVariable String templateId,
            @PathVariable String deliverableTemplateId,
            @Valid @RequestBody ProjectTemplateDtos.UpdateDeliverableTemplateRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ProjectTemplateDtos.ProjectTemplateDeliverableTemplateResponse.from(
                updateDeliverableTemplateUseCase.execute(
                        templateId,
                        deliverableTemplateId,
                        new UpdateDeliverableTemplateUseCase.UpdateDeliverableTemplateCommand(
                                request.phaseTemplateId(),
                                request.milestoneTemplateId(),
                                request.code(),
                                request.name(),
                                request.description(),
                                request.deliverableType(),
                                request.requiredDocument(),
                                request.plannedDueOffsetDays(),
                                request.appliesToType(),
                                request.structureLevelTemplateId(),
                                request.responsibleOrganizationRole(),
                                request.approverOrganizationRole(),
                                request.visibilityScope(),
                                request.priority()),
                        actor));
    }
}

