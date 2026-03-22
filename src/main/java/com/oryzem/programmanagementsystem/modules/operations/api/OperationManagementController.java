package com.oryzem.programmanagementsystem.modules.operations.api;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.modules.operations.CreateOperationRequest;
import com.oryzem.programmanagementsystem.modules.operations.OperationActionResponse;
import com.oryzem.programmanagementsystem.modules.operations.OperationManagementService;
import com.oryzem.programmanagementsystem.modules.operations.OperationResponse;
import com.oryzem.programmanagementsystem.modules.operations.UpdateOperationRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationManagementController {

    private final OperationManagementService operationManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public OperationManagementController(
            OperationManagementService operationManagementService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.operationManagementService = operationManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping
    public List<OperationResponse> listOperations(
            Authentication authentication,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.listOperations(actor, tenantId, supportOverride, justification);
    }

    @PostMapping
    public ResponseEntity<OperationResponse> createOperation(
            Authentication authentication,
            @Valid @RequestBody CreateOperationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        OperationResponse created = operationManagementService.createOperation(actor, request);
        return ResponseEntity.created(URI.create("/api/operations/" + created.id())).body(created);
    }

    @PutMapping("/{operationId}")
    public OperationResponse updateOperation(
            Authentication authentication,
            @PathVariable String operationId,
            @Valid @RequestBody UpdateOperationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.updateOperation(actor, operationId, request);
    }

    @DeleteMapping("/{operationId}")
    public ResponseEntity<Void> deleteOperation(
            Authentication authentication,
            @PathVariable String operationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        operationManagementService.deleteOperation(actor, operationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{operationId}/submit")
    public OperationActionResponse submitOperation(
            Authentication authentication,
            @PathVariable String operationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.submitOperation(actor, operationId);
    }

    @PostMapping("/{operationId}/approve")
    public OperationActionResponse approveOperation(
            Authentication authentication,
            @PathVariable String operationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.approveOperation(actor, operationId);
    }

    @PostMapping("/{operationId}/reject")
    public OperationActionResponse rejectOperation(
            Authentication authentication,
            @PathVariable String operationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.rejectOperation(actor, operationId);
    }

    @PostMapping("/{operationId}/reopen")
    public OperationActionResponse reopenOperation(
            Authentication authentication,
            @PathVariable String operationId,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.reopenOperation(actor, operationId, justification);
    }

    @PostMapping("/{operationId}/reprocess")
    public OperationActionResponse reprocessOperation(
            Authentication authentication,
            @PathVariable String operationId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return operationManagementService.reprocessOperation(actor, operationId, supportOverride, justification);
    }
}

