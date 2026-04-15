package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ApproveDeliverableSubmissionUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.GetDeliverableSubmissionDetailUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListDeliverableSubmissionsUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.RejectDeliverableSubmissionUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.SubmitDeliverableUseCase;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/deliverables/{deliverableId}/submissions")
public class DeliverableSubmissionController {

    private final ProjectApiSupport apiSupport;
    private final SubmitDeliverableUseCase submitDeliverableUseCase;
    private final ListDeliverableSubmissionsUseCase listDeliverableSubmissionsUseCase;
    private final GetDeliverableSubmissionDetailUseCase getDeliverableSubmissionDetailUseCase;
    private final ApproveDeliverableSubmissionUseCase approveDeliverableSubmissionUseCase;
    private final RejectDeliverableSubmissionUseCase rejectDeliverableSubmissionUseCase;

    public DeliverableSubmissionController(
            ProjectApiSupport apiSupport,
            SubmitDeliverableUseCase submitDeliverableUseCase,
            ListDeliverableSubmissionsUseCase listDeliverableSubmissionsUseCase,
            GetDeliverableSubmissionDetailUseCase getDeliverableSubmissionDetailUseCase,
            ApproveDeliverableSubmissionUseCase approveDeliverableSubmissionUseCase,
            RejectDeliverableSubmissionUseCase rejectDeliverableSubmissionUseCase) {
        this.apiSupport = apiSupport;
        this.submitDeliverableUseCase = submitDeliverableUseCase;
        this.listDeliverableSubmissionsUseCase = listDeliverableSubmissionsUseCase;
        this.getDeliverableSubmissionDetailUseCase = getDeliverableSubmissionDetailUseCase;
        this.approveDeliverableSubmissionUseCase = approveDeliverableSubmissionUseCase;
        this.rejectDeliverableSubmissionUseCase = rejectDeliverableSubmissionUseCase;
    }

    @PostMapping
    public ResponseEntity<DeliverableSubmissionDtos.DeliverableSubmissionResponse> submitDeliverable(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId, @RequestBody DeliverableSubmissionDtos.SubmitDeliverableRequest request, @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        DeliverableSubmissionDtos.DeliverableSubmissionResponse response = DeliverableSubmissionDtos.DeliverableSubmissionResponse.from(submitDeliverableUseCase.execute(projectId, deliverableId, new SubmitDeliverableUseCase.SubmitDeliverableCommand(request.deliverableVersion(), request.documentIds()), apiSupport.actor(authentication), idempotencyKey));
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions/" + response.id())).body(response);
    }

    @GetMapping
    public List<DeliverableSubmissionDtos.DeliverableSubmissionResponse> listSubmissions(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId) {
        return listDeliverableSubmissionsUseCase.execute(projectId, deliverableId, apiSupport.actor(authentication)).stream().map(DeliverableSubmissionDtos.DeliverableSubmissionResponse::from).toList();
    }

    @GetMapping("/{submissionId}")
    public DeliverableSubmissionDtos.DeliverableSubmissionResponse getSubmission(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId, @PathVariable String submissionId) {
        return DeliverableSubmissionDtos.DeliverableSubmissionResponse.from(getDeliverableSubmissionDetailUseCase.execute(projectId, deliverableId, submissionId, apiSupport.actor(authentication)));
    }

    @PostMapping("/{submissionId}/approve")
    public DeliverableSubmissionDtos.DeliverableSubmissionResponse approveSubmission(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId, @PathVariable String submissionId, @RequestBody DeliverableSubmissionDtos.ReviewSubmissionRequest request, @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return DeliverableSubmissionDtos.DeliverableSubmissionResponse.from(approveDeliverableSubmissionUseCase.execute(projectId, deliverableId, submissionId, new ApproveDeliverableSubmissionUseCase.ReviewSubmissionCommand(request.reviewComment(), request.version()), apiSupport.actor(authentication), idempotencyKey));
    }

    @PostMapping("/{submissionId}/reject")
    public DeliverableSubmissionDtos.DeliverableSubmissionResponse rejectSubmission(Authentication authentication, @PathVariable String projectId, @PathVariable String deliverableId, @PathVariable String submissionId, @RequestBody DeliverableSubmissionDtos.ReviewSubmissionRequest request, @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return DeliverableSubmissionDtos.DeliverableSubmissionResponse.from(rejectDeliverableSubmissionUseCase.execute(projectId, deliverableId, submissionId, new RejectDeliverableSubmissionUseCase.ReviewSubmissionCommand(request.reviewComment(), request.version()), apiSupport.actor(authentication), idempotencyKey));
    }
}

