package com.oryzem.programmanagementsystem.modules.operations;

import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OperationManagementService {

    private final OperationRepository operationRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final AccessContextService accessContextService;

    public OperationManagementService(
            OperationRepository operationRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            AccessContextService accessContextService) {
        this.operationRepository = operationRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.accessContextService = accessContextService;
    }

    public List<OperationResponse> listOperations(
            AuthenticatedUser actor,
            String tenantId,
            boolean supportOverride,
            String justification) {
        String effectiveTenantId = resolveListTenantId(actor, tenantId);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.VIEW)
                .resourceTenantId(effectiveTenantId)
                .resourceTenantType(resolveListTenantType(actor, effectiveTenantId))
                .auditTrailEnabled(shouldAuditView(actor, effectiveTenantId, supportOverride, justification))
                .supportOverride(supportOverride)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);

        if (decision.auditRequired() || (decision.crossTenant() && actor.hasRole(Role.SUPPORT))) {
            recordAudit(actor, effectiveTenantId, "OPERATIONS_VIEW", null, justification, decision.crossTenant());
        }

        List<OperationRecord> operations = effectiveTenantId == null
                ? operationRepository.findAll()
                : operationRepository.findByTenantId(effectiveTenantId);
        return operations.stream().map(OperationResponse::from).toList();
    }

    public OperationResponse createOperation(AuthenticatedUser actor, CreateOperationRequest request) {
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.CREATE)
                .resourceTenantId(request.tenantId())
                .resourceTenantType(request.tenantType())
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        String canonicalTenantId = accessContextService.canonicalTenantId(request.tenantId());
        TenantType tenantType = request.tenantType() != null
                ? request.tenantType()
                : accessContextService.resolveTenantType(canonicalTenantId).orElse(actor.tenantType());
        enforceTenantScope(actor, canonicalTenantId, tenantType);

        Instant now = Instant.now();
        OperationRecord created = new OperationRecord(
                "OP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(),
                request.title().trim(),
                normalizeDescription(request.description()),
                canonicalTenantId,
                tenantType,
                actor.subject(),
                OperationStatus.DRAFT,
                now,
                now,
                null,
                null,
                null,
                null,
                null);

        return OperationResponse.from(operationRepository.save(created));
    }

    public OperationResponse updateOperation(AuthenticatedUser actor, String operationId, UpdateOperationRequest request) {
        OperationRecord operation = findRequiredOperation(operationId);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.EDIT)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        OperationRecord updated = operation.withContent(request.title().trim(), normalizeDescription(request.description()), Instant.now());
        return OperationResponse.from(operationRepository.save(updated));
    }

    public void deleteOperation(AuthenticatedUser actor, String operationId) {
        OperationRecord operation = findRequiredOperation(operationId);
        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.DELETE)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        operationRepository.deleteById(operationId);
    }

    public OperationActionResponse submitOperation(AuthenticatedUser actor, String operationId) {
        OperationRecord operation = findRequiredOperation(operationId);
        requireStatus(operation, OperationStatus.DRAFT, OperationStatus.RETURNED);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.SUBMIT)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        Instant now = Instant.now();
        OperationRecord updated = operation.withStatus(OperationStatus.SUBMITTED, now);
        operationRepository.save(updated);
        return new OperationActionResponse(updated.id(), Action.SUBMIT.name(), updated.status(), now, "OK");
    }

    public OperationActionResponse approveOperation(AuthenticatedUser actor, String operationId) {
        return performManagerialAction(actor, operationId, Action.APPROVE, OperationStatus.APPROVED, OperationStatus.SUBMITTED);
    }

    public OperationActionResponse rejectOperation(AuthenticatedUser actor, String operationId) {
        return performManagerialAction(actor, operationId, Action.REJECT, OperationStatus.REJECTED, OperationStatus.SUBMITTED);
    }

    public OperationActionResponse reopenOperation(AuthenticatedUser actor, String operationId, String justification) {
        OperationRecord operation = findRequiredOperation(operationId);
        requireStatus(operation, OperationStatus.APPROVED, OperationStatus.REJECTED);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.REOPEN)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .auditTrailEnabled(true)
                .justification(justification)
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        Instant now = Instant.now();
        OperationRecord updated = operation.withStatus(OperationStatus.RETURNED, now);
        operationRepository.save(updated);
        if (decision.auditRequired()) {
            recordAudit(actor, updated.tenantId(), Action.REOPEN.name(), updated.id(), justification, decision.crossTenant());
        }
        return new OperationActionResponse(updated.id(), Action.REOPEN.name(), updated.status(), now, "OK");
    }

    public OperationActionResponse reprocessOperation(
            AuthenticatedUser actor,
            String operationId,
            boolean supportOverride,
            String justification) {
        OperationRecord operation = findRequiredOperation(operationId);
        requireStatus(operation, OperationStatus.APPROVED, OperationStatus.REJECTED, OperationStatus.SUBMITTED);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, Action.REPROCESS)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .auditTrailEnabled(true)
                .supportOverride(supportOverride)
                .justification(justification)
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        Instant now = Instant.now();
        OperationRecord updated = operation.withStatus(OperationStatus.REPROCESSING, now);
        operationRepository.save(updated);
        recordAudit(actor, updated.tenantId(), Action.REPROCESS.name(), updated.id(), justification, decision.crossTenant());
        return new OperationActionResponse(updated.id(), Action.REPROCESS.name(), updated.status(), now, "OK");
    }

    private OperationActionResponse performManagerialAction(
            AuthenticatedUser actor,
            String operationId,
            Action action,
            OperationStatus newStatus,
            OperationStatus requiredStatus) {
        OperationRecord operation = findRequiredOperation(operationId);
        requireStatus(operation, requiredStatus);

        AuthorizationContext context = AuthorizationContext.builder(AppModule.OPERATIONS, action)
                .resourceTenantId(operation.tenantId())
                .resourceTenantType(operation.tenantType())
                .resourceOwnerUserId(operation.createdBy())
                .resourceStatus(operation.status().name())
                .resourceId(operation.id())
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        assertAllowed(decision);
        enforceTenantScope(actor, operation.tenantId(), operation.tenantType(), decision.crossTenant());

        Instant now = Instant.now();
        OperationRecord updated = operation.withStatus(newStatus, now);
        operationRepository.save(updated);
        return new OperationActionResponse(updated.id(), action.name(), updated.status(), now, "OK");
    }

    private OperationRecord findRequiredOperation(String operationId) {
        return operationRepository.findById(operationId).orElseThrow(() -> new OperationNotFoundException(operationId));
    }

    private void requireStatus(OperationRecord operation, OperationStatus... allowedStatuses) {
        for (OperationStatus allowedStatus : allowedStatuses) {
            if (operation.status() == allowedStatus) {
                return;
            }
        }
        throw new IllegalArgumentException("Operation status does not allow the requested action.");
    }

    private void assertAllowed(AuthorizationDecision decision) {
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
    }

    private void enforceTenantScope(AuthenticatedUser actor, String targetTenantId, TenantType targetTenantType) {
        enforceTenantScope(actor, targetTenantId, targetTenantType, false);
    }

    private void enforceTenantScope(
            AuthenticatedUser actor,
            String targetTenantId,
            TenantType targetTenantType,
            boolean crossTenantAllowed) {
        if (actor.isAdmin() || crossTenantAllowed) {
            return;
        }

        String actorTenantId = accessContextService.canonicalTenantId(actor.tenantId());
        String effectiveTargetTenantId = accessContextService.canonicalTenantId(targetTenantId);
        if (actorTenantId != null && !actorTenantId.equals(effectiveTargetTenantId)) {
            throw new AccessDeniedException("Tenant scope mismatch for requested operation.");
        }

        if (actor.tenantType() != null && targetTenantType != null && actor.tenantType() != targetTenantType) {
            throw new AccessDeniedException("Tenant type mismatch for requested operation.");
        }
    }

    private String resolveListTenantId(AuthenticatedUser actor, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return accessContextService.canonicalTenantId(tenantId);
        }
        return actor.isAdmin() ? null : accessContextService.canonicalTenantId(actor.tenantId());
    }

    private TenantType resolveListTenantType(AuthenticatedUser actor, String effectiveTenantId) {
        if (effectiveTenantId == null) {
            return null;
        }
        return operationRepository.findByTenantId(effectiveTenantId).stream()
                .map(OperationRecord::tenantType)
                .findFirst()
                .orElse(actor.tenantType());
    }

    private boolean shouldAuditView(
            AuthenticatedUser actor,
            String effectiveTenantId,
            boolean supportOverride,
            String justification) {
        if (!actor.hasRole(Role.SUPPORT)) {
            return false;
        }

        return effectiveTenantId != null
                && accessContextService.canonicalTenantId(actor.tenantId()) != null
                && !accessContextService.canonicalTenantId(actor.tenantId()).equals(effectiveTenantId)
                && supportOverride
                && justification != null
                && !justification.isBlank();
    }

    private void recordAudit(
            AuthenticatedUser actor,
            String tenantId,
            String action,
            String resourceId,
            String justification,
            boolean crossTenant) {
        auditTrailService.record(new AuditTrailEvent(
                null,
                action,
                actor.subject(),
                primaryRole(actor),
                actor.tenantId(),
                tenantId,
                "OPERATION",
                resourceId,
                justification,
                metadataJson(crossTenant),
                crossTenant,
                null,
                AppModule.OPERATIONS.name(),
                Instant.now()));
    }

    private Role primaryRole(AuthenticatedUser actor) {
        return actor.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private String metadataJson(boolean crossTenant) {
        return "{\"crossTenant\":" + crossTenant + "}";
    }
}

