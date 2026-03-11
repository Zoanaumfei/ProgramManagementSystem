package com.oryzem.programmanagementsystem.reports;

import com.oryzem.programmanagementsystem.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.authorization.Action;
import com.oryzem.programmanagementsystem.authorization.AppModule;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import com.oryzem.programmanagementsystem.operations.OperationRecord;
import com.oryzem.programmanagementsystem.operations.OperationRepository;
import com.oryzem.programmanagementsystem.operations.OperationStatus;
import com.oryzem.programmanagementsystem.users.ManagedUser;
import com.oryzem.programmanagementsystem.users.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ReportManagementService {

    private static final String MASKED_VALUE = "[MASKED]";

    private final UserRepository userRepository;
    private final OperationRepository operationRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;

    public ReportManagementService(
            UserRepository userRepository,
            OperationRepository operationRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService) {
        this.userRepository = userRepository;
        this.operationRepository = operationRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
    }

    public ReportSummaryResponse getSummary(
            AuthenticatedUser actor,
            String tenantId,
            boolean supportOverride,
            String justification) {
        String effectiveTenantId = resolveRequestedTenant(actor, tenantId);
        TenantType effectiveTenantType = resolveTenantType(actor, effectiveTenantId);

        AuthorizationDecision decision = authorize(
                actor,
                Action.VIEW,
                effectiveTenantId,
                effectiveTenantType,
                false,
                false,
                supportOverride,
                justification);

        List<ManagedUser> users = selectUsers(effectiveTenantId);
        List<OperationRecord> operations = selectOperations(effectiveTenantId, null);
        Instant generatedAt = Instant.now();

        recordAuditIfNeeded(actor, decision, effectiveTenantId, "REPORT_SUMMARY_VIEW", justification);

        return ReportSummaryResponse.of(
                generatedAt,
                effectiveTenantId,
                effectiveTenantType,
                users.size(),
                operations.size(),
                countUsersByRole(users),
                countUsersByStatus(users),
                countOperationsByStatus(operations));
    }

    public OperationsReportResponse getOperationsReport(
            AuthenticatedUser actor,
            String tenantId,
            String status,
            boolean supportOverride,
            String justification) {
        String effectiveTenantId = resolveRequestedTenant(actor, tenantId);
        TenantType effectiveTenantType = resolveTenantType(actor, effectiveTenantId);
        OperationStatus statusFilter = parseStatus(status);

        AuthorizationDecision decision = authorize(
                actor,
                Action.VIEW,
                effectiveTenantId,
                effectiveTenantType,
                false,
                false,
                supportOverride,
                justification);

        List<OperationRecord> operations = selectOperations(effectiveTenantId, statusFilter);
        recordAuditIfNeeded(actor, decision, effectiveTenantId, "REPORT_OPERATIONS_VIEW", justification);

        List<OperationsReportItem> items = operations.stream()
                .map(operation -> new OperationsReportItem(
                        operation.id(),
                        operation.title(),
                        operation.tenantId(),
                        operation.tenantType(),
                        operation.status(),
                        operation.createdAt(),
                        operation.updatedAt()))
                .toList();

        return new OperationsReportResponse(
                Instant.now(),
                effectiveTenantId,
                effectiveTenantType,
                statusFilter == null ? null : statusFilter.name(),
                items.size(),
                items);
    }

    public OperationsExportResponse exportOperationsReport(
            AuthenticatedUser actor,
            String tenantId,
            String status,
            boolean includeSensitiveData,
            boolean maskedView,
            boolean supportOverride,
            String justification) {
        String effectiveTenantId = resolveRequestedTenant(actor, tenantId);
        TenantType effectiveTenantType = resolveTenantType(actor, effectiveTenantId);
        OperationStatus statusFilter = parseStatus(status);

        AuthorizationDecision decision = authorize(
                actor,
                Action.EXPORT,
                effectiveTenantId,
                effectiveTenantType,
                includeSensitiveData,
                maskedView,
                supportOverride,
                justification);

        List<ManagedUser> users = selectUsers(effectiveTenantId);
        List<OperationRecord> operations = selectOperations(effectiveTenantId, statusFilter);
        recordAuditIfNeeded(actor, decision, effectiveTenantId, "REPORT_OPERATIONS_EXPORT", justification);

        ReportSummaryResponse summary = ReportSummaryResponse.of(
                Instant.now(),
                effectiveTenantId,
                effectiveTenantType,
                users.size(),
                operations.size(),
                countUsersByRole(users),
                countUsersByStatus(users),
                countOperationsByStatus(operations));

        List<OperationsExportItem> items = operations.stream()
                .map(operation -> new OperationsExportItem(
                        operation.id(),
                        operation.title(),
                        operation.status(),
                        operation.createdAt(),
                        operation.updatedAt(),
                        exportCreatedBy(operation, includeSensitiveData, maskedView),
                        exportDescription(operation, includeSensitiveData, maskedView)))
                .toList();

        return new OperationsExportResponse(
                Instant.now(),
                effectiveTenantId,
                effectiveTenantType,
                statusFilter == null ? null : statusFilter.name(),
                includeSensitiveData,
                includeSensitiveData && maskedView,
                summary,
                items);
    }

    private AuthorizationDecision authorize(
            AuthenticatedUser actor,
            Action action,
            String tenantId,
            TenantType tenantType,
            boolean includeSensitiveData,
            boolean maskedView,
            boolean supportOverride,
            String justification) {
        AuthorizationContext context = AuthorizationContext.builder(AppModule.REPORTS, action)
                .resourceTenantId(tenantId)
                .resourceTenantType(tenantType)
                .auditTrailEnabled(shouldEnableAudit(actor, tenantId, supportOverride, justification))
                .supportOverride(supportOverride)
                .justification(justification)
                .sensitiveDataRequested(includeSensitiveData)
                .maskedViewRequested(maskedView)
                .build();
        AuthorizationDecision decision = authorizationService.decide(actor, context);
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }
        return decision;
    }

    private List<ManagedUser> selectUsers(String tenantId) {
        return tenantId == null ? userRepository.findAll() : userRepository.findByTenantId(tenantId);
    }

    private List<OperationRecord> selectOperations(String tenantId, OperationStatus statusFilter) {
        List<OperationRecord> operations = tenantId == null
                ? operationRepository.findAll()
                : operationRepository.findByTenantId(tenantId);
        if (statusFilter == null) {
            return operations;
        }
        return operations.stream()
                .filter(operation -> operation.status() == statusFilter)
                .toList();
    }

    private String resolveRequestedTenant(AuthenticatedUser actor, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.trim();
        }
        return actor.isAdmin() ? null : actor.tenantId();
    }

    private TenantType resolveTenantType(AuthenticatedUser actor, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        return userRepository.findByTenantId(tenantId).stream()
                .map(ManagedUser::tenantType)
                .findFirst()
                .or(() -> operationRepository.findByTenantId(tenantId).stream().map(OperationRecord::tenantType).findFirst())
                .orElse(actor.tenantType());
    }

    private OperationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return OperationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported operation status filter.");
        }
    }

    private Map<String, Long> countUsersByRole(List<ManagedUser> users) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            counts.put(role.name(), users.stream().filter(user -> user.role() == role).count());
        }
        return counts;
    }

    private Map<String, Long> countUsersByStatus(List<ManagedUser> users) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (com.oryzem.programmanagementsystem.users.UserStatus status : com.oryzem.programmanagementsystem.users.UserStatus.values()) {
            counts.put(status.name(), users.stream().filter(user -> user.status() == status).count());
        }
        return counts;
    }

    private Map<String, Long> countOperationsByStatus(List<OperationRecord> operations) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OperationStatus status : OperationStatus.values()) {
            counts.put(status.name(), operations.stream().filter(operation -> operation.status() == status).count());
        }
        return counts;
    }

    private void recordAuditIfNeeded(
            AuthenticatedUser actor,
            AuthorizationDecision decision,
            String tenantId,
            String action,
            String justification) {
        if (!decision.crossTenant() && !decision.auditRequired()) {
            return;
        }

        auditTrailService.record(new AuditTrailEvent(
                null,
                action,
                actor.subject(),
                primaryRole(actor),
                actor.tenantId(),
                tenantId,
                "REPORT",
                null,
                justification,
                metadataJson(decision.crossTenant()),
                decision.crossTenant(),
                null,
                AppModule.REPORTS.name(),
                Instant.now()));
    }

    private Role primaryRole(AuthenticatedUser actor) {
        return actor.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private boolean shouldEnableAudit(
            AuthenticatedUser actor,
            String tenantId,
            boolean supportOverride,
            String justification) {
        if (!actor.hasRole(Role.SUPPORT)) {
            return false;
        }

        return tenantId != null
                && actor.tenantId() != null
                && !actor.tenantId().equals(tenantId)
                && supportOverride
                && justification != null
                && !justification.isBlank();
    }

    private String exportCreatedBy(OperationRecord operation, boolean includeSensitiveData, boolean maskedView) {
        if (!includeSensitiveData) {
            return null;
        }
        if (!maskedView) {
            return operation.createdBy();
        }
        return mask(operation.createdBy());
    }

    private String exportDescription(OperationRecord operation, boolean includeSensitiveData, boolean maskedView) {
        if (!includeSensitiveData) {
            return null;
        }
        if (!maskedView) {
            return operation.description();
        }
        return MASKED_VALUE;
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return MASKED_VALUE;
        }
        if (value.length() <= 4) {
            return MASKED_VALUE;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private String metadataJson(boolean crossTenant) {
        return "{\"crossTenant\":" + crossTenant + "}";
    }
}
