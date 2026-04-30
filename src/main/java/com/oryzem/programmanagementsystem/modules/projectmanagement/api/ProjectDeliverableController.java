package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.CreateDeliverableUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.DeleteDeliverableUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetProjectDeliverableDetailUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.UpdateDeliverableUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListPendingSubmissionReviewsQuery;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListProjectDeliverablesQuery;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.ListResponsibleDeliverablesQuery;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/projects/{projectId}/deliverables")
public class ProjectDeliverableController {

    private final ProjectApiSupport apiSupport;
    private final ListProjectDeliverablesQuery listProjectDeliverablesQuery;
    private final GetProjectDeliverableDetailUseCase getProjectDeliverableDetailUseCase;
    private final CreateDeliverableUseCase createDeliverableUseCase;
    private final UpdateDeliverableUseCase updateDeliverableUseCase;
    private final DeleteDeliverableUseCase deleteDeliverableUseCase;
    private final ListPendingSubmissionReviewsQuery listPendingSubmissionReviewsQuery;
    private final ListResponsibleDeliverablesQuery listResponsibleDeliverablesQuery;

    public ProjectDeliverableController(
            ProjectApiSupport apiSupport,
            ListProjectDeliverablesQuery listProjectDeliverablesQuery,
            GetProjectDeliverableDetailUseCase getProjectDeliverableDetailUseCase,
            CreateDeliverableUseCase createDeliverableUseCase,
            UpdateDeliverableUseCase updateDeliverableUseCase,
            DeleteDeliverableUseCase deleteDeliverableUseCase,
            ListPendingSubmissionReviewsQuery listPendingSubmissionReviewsQuery,
            ListResponsibleDeliverablesQuery listResponsibleDeliverablesQuery) {
        this.apiSupport = apiSupport;
        this.listProjectDeliverablesQuery = listProjectDeliverablesQuery;
        this.getProjectDeliverableDetailUseCase = getProjectDeliverableDetailUseCase;
        this.createDeliverableUseCase = createDeliverableUseCase;
        this.updateDeliverableUseCase = updateDeliverableUseCase;
        this.deleteDeliverableUseCase = deleteDeliverableUseCase;
        this.listPendingSubmissionReviewsQuery = listPendingSubmissionReviewsQuery;
        this.listResponsibleDeliverablesQuery = listResponsibleDeliverablesQuery;
    }

    @GetMapping
    public List<ProjectArtifactDtos.ProjectDeliverableResponse> listDeliverables(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return listProjectDeliverablesQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)).stream().map(ProjectArtifactDtos.ProjectDeliverableResponse::from).toList();
    }

    @GetMapping("/{deliverableId}")
    public ProjectArtifactDtos.ProjectDeliverableResponse getDeliverable(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId) {
        return ProjectArtifactDtos.ProjectDeliverableResponse.from(getProjectDeliverableDetailUseCase.execute(projectId, deliverableId, apiSupport.actor(authentication)));
    }

    @PostMapping
    public ProjectArtifactDtos.ProjectDeliverableResponse createDeliverable(Authentication authentication, @PathVariable String projectId, @RequestBody ProjectArtifactDtos.CreateDeliverableRequest request) {
        return ProjectArtifactDtos.ProjectDeliverableResponse.from(createDeliverableUseCase.execute(projectId, new CreateDeliverableUseCase.CreateDeliverableCommand(request.structureNodeId(), request.phaseId(), request.milestoneId(), request.code(), request.name(), request.description(), request.deliverableType(), request.responsibleOrganizationId(), request.responsibleUserId(), request.approverOrganizationId(), request.approverUserId(), request.requiredDocument(), request.plannedDueDate(), request.priority(), request.visibilityScope()), apiSupport.actor(authentication)));
    }

    @PatchMapping("/{deliverableId}")
    public ProjectArtifactDtos.ProjectDeliverableResponse updateDeliverable(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId, @RequestBody ProjectArtifactDtos.UpdateDeliverableRequest request) {
        return ProjectArtifactDtos.ProjectDeliverableResponse.from(updateDeliverableUseCase.execute(projectId, deliverableId, new UpdateDeliverableUseCase.UpdateDeliverableCommand(request.code(), request.name(), request.description(), request.deliverableType(), request.responsibleOrganizationId(), request.responsibleUserId(), request.approverOrganizationId(), request.approverUserId(), request.requiredDocument(), request.plannedDueDate(), request.status(), request.priority(), request.visibilityScope(), request.version()), apiSupport.actor(authentication)));
    }

    @DeleteMapping("/{deliverableId}")
    public ResponseEntity<Void> deleteDeliverable(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId) {
        deleteDeliverableUseCase.execute(projectId, deliverableId, apiSupport.actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-review")
    public List<ProjectArtifactDtos.ProjectDeliverableResponse> listPendingReview(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return listPendingSubmissionReviewsQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)).stream().map(ProjectArtifactDtos.ProjectDeliverableResponse::from).toList();
    }

    @GetMapping("/responsible")
    public List<ProjectArtifactDtos.ProjectDeliverableResponse> listResponsible(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return listResponsibleDeliverablesQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)).stream().map(ProjectArtifactDtos.ProjectDeliverableResponse::from).toList();
    }
}

