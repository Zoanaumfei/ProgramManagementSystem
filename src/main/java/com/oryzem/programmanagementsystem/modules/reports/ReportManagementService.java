package com.oryzem.programmanagementsystem.modules.reports;

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
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import com.oryzem.programmanagementsystem.modules.operations.OperationStatus;
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
    private static final List<String> USER_STATUSES = List.of("INVITED", "ACTIVE", "INACTIVE");
    private static final List<String> OPERATION_STATUSES = List.of(
            "DRAFT",
            "SUBMITTED",
            "APPROVED",
            "REJECTED",
            "RETURNED",
            "REPROCESSING");

    private final ReportUserQueryPort reportUserQueryPort;
    private final ReportOperationQueryPort reportOperationQueryPort;
    private final OrganizationLookup organizationLookup;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final AccessContextService accessContextService;

    public ReportManagementService(
            ReportUserQueryPort reportUserQueryPort,
            ReportOperationQueryPort reportOperationQueryPort,
            OrganizationLookup organizationLookup,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            AccessContextService accessContextService) {
        this.reportUserQueryPort = reportUserQueryPort;
        this.reportOperationQueryPort = reportOperationQueryPort;
        this.organizationLookup = organizationLookup;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.accessContextService = accessContextService;
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

        List<ReportUserQueryPort.ReportUserView> users = reportUserQueryPort.findUsers(effectiveTenantId);
        List<ReportOperationQueryPort.ReportOperationView> operations =
                reportOperationQueryPort.findOperations(effectiveTenantId, null);
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
        String statusFilter = parseStatus(status);

        AuthorizationDecision decision = authorize(
                actor,
                Action.VIEW,
                effectiveTenantId,
                effectiveTenantType,
                false,
                false,
                supportOverride,
                justification);

        List<ReportOperationQueryPort.ReportOperationView> operations =
                reportOperationQueryPort.findOperations(effectiveTenantId, statusFilter);
        recordAuditIfNeeded(actor, decision, effectiveTenantId, "REPORT_OPERATIONS_VIEW", justification);

        List<OperationsReportItem> items = operations.stream()
                .map(operation -> new OperationsReportItem(
                        operation.id(),
                        operation.title(),
                        operation.tenantId(),
                        operation.tenantType(),
                        OperationStatus.valueOf(operation.status()),
                        operation.createdAt(),
                        operation.updatedAt()))
                .toList();

        return new OperationsReportResponse(
                Instant.now(),
                effectiveTenantId,
                effectiveTenantType,
                statusFilter,
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
        String statusFilter = parseStatus(status);

        AuthorizationDecision decision = authorize(
                actor,
                Action.EXPORT,
                effectiveTenantId,
                effectiveTenantType,
                includeSensitiveData,
                maskedView,
                supportOverride,
                justification);

        List<ReportUserQueryPort.ReportUserView> users = reportUserQueryPort.findUsers(effectiveTenantId);
        List<ReportOperationQueryPort.ReportOperationView> operations =
                reportOperationQueryPort.findOperations(effectiveTenantId, statusFilter);
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
                        OperationStatus.valueOf(operation.status()),
                        operation.createdAt(),
                        operation.updatedAt(),
                        exportCreatedBy(operation, includeSensitiveData, maskedView),
                        exportDescription(operation, includeSensitiveData, maskedView)))
                .toList();

        return new OperationsExportResponse(
                Instant.now(),
                effectiveTenantId,
                effectiveTenantType,
                statusFilter,
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

    private String resolveRequestedTenant(AuthenticatedUser actor, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return accessContextService.canonicalTenantId(tenantId);
        }
        return actor.isAdmin() ? null : accessContextService.canonicalTenantId(actor.tenantId());
    }

    private TenantType resolveTenantType(AuthenticatedUser actor, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        return accessContextService.resolveTenantType(tenantId)
                .or(() -> organizationLookup.findById(tenantId)
                .map(OrganizationLookup.OrganizationView::tenantType)
                )
                .orElse(actor.tenantType());
    }

    private String parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!OPERATION_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported operation status filter.");
        }
        return normalizedStatus;
    }

    private Map<String, Long> countUsersByRole(List<ReportUserQueryPort.ReportUserView> users) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            counts.put(role.name(), users.stream().filter(user -> user.role() == role).count());
        }
        return counts;
    }

    private Map<String, Long> countUsersByStatus(List<ReportUserQueryPort.ReportUserView> users) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : USER_STATUSES) {
            counts.put(status, users.stream().filter(user -> status.equals(user.status())).count());
        }
        return counts;
    }

    private Map<String, Long> countOperationsByStatus(List<ReportOperationQueryPort.ReportOperationView> operations) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : OPERATION_STATUSES) {
            counts.put(status, operations.stream().filter(operation -> status.equals(operation.status())).count());
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
                && accessContextService.canonicalTenantId(actor.tenantId()) != null
                && !accessContextService.canonicalTenantId(actor.tenantId()).equals(tenantId)
                && supportOverride
                && justification != null
                && !justification.isBlank();
    }

    private String exportCreatedBy(
            ReportOperationQueryPort.ReportOperationView operation,
            boolean includeSensitiveData,
            boolean maskedView) {
        if (!includeSensitiveData) {
            return null;
        }
        if (!maskedView) {
            return operation.createdBy();
        }
        return mask(operation.createdBy());
    }

    private String exportDescription(
            ReportOperationQueryPort.ReportOperationView operation,
            boolean includeSensitiveData,
            boolean maskedView) {
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
