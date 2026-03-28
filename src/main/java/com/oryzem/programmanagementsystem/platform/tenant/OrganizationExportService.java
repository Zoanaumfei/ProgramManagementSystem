package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationExportService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessService organizationAccessService;
    private final AuditTrailService auditTrailService;

    OrganizationExportService(
            OrganizationRepository organizationRepository,
            OrganizationAccessService organizationAccessService,
            AuditTrailService auditTrailService) {
        this.organizationRepository = organizationRepository;
        this.organizationAccessService = organizationAccessService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    OrganizationExportResponse getExportStatus(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = organizationAccessService.findManagedOrganization(organizationId);
        organizationAccessService.assertCanAccessOrganization(
                actor,
                organization,
                Action.CONFIGURE,
                actor.hasRole(Role.SUPPORT),
                "view-export-status");
        return toResponse(organization);
    }

    OrganizationExportResponse requestExport(
            String organizationId,
            OrganizationExportRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = organizationAccessService.findManagedOrganization(organizationId);
        organizationAccessService.assertCanAccessOrganization(
                actor,
                organization,
                Action.CONFIGURE,
                actor.hasRole(Role.SUPPORT),
                request.justification());
        assertEligibleForExport(organization);
        if (organization.getDataExportStatus() != OrganizationDataExportStatus.READY_FOR_EXPORT) {
            throw new ConflictException("Organization export is not ready to start.");
        }

        Instant now = Instant.now();
        organization.markExportInProgress(actor.username(), now);
        OrganizationEntity saved = organizationRepository.save(organization);
        recordAudit(
                actor,
                saved.getTenantId(),
                "ORGANIZATION_EXPORT_REQUESTED",
                saved.getId(),
                "{\"from\":\"READY_FOR_EXPORT\",\"to\":\"EXPORT_IN_PROGRESS\",\"justification\":\""
                        + escapeJson(request.justification()) + "\"}");
        return toResponse(saved);
    }

    OrganizationExportResponse completeExport(
            String organizationId,
            OrganizationExportRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = organizationAccessService.findManagedOrganization(organizationId);
        organizationAccessService.assertCanAccessOrganization(
                actor,
                organization,
                Action.CONFIGURE,
                actor.hasRole(Role.SUPPORT),
                request.justification());
        assertEligibleForExport(organization);
        if (organization.getDataExportStatus() != OrganizationDataExportStatus.EXPORT_IN_PROGRESS) {
            throw new ConflictException("Organization export is not in progress.");
        }

        Instant now = Instant.now();
        organization.markExported(actor.username(), now);
        OrganizationEntity saved = organizationRepository.save(organization);
        recordAudit(
                actor,
                saved.getTenantId(),
                "ORGANIZATION_EXPORT_COMPLETED",
                saved.getId(),
                "{\"from\":\"EXPORT_IN_PROGRESS\",\"to\":\"EXPORTED\",\"justification\":\""
                        + escapeJson(request.justification()) + "\"}");
        return toResponse(saved);
    }

    private void assertEligibleForExport(OrganizationEntity organization) {
        if (organization.getLifecycleState() != OrganizationLifecycleState.OFFBOARDED) {
            throw new ConflictException("Only offboarded organizations can be exported.");
        }
        Instant retentionUntil = organization.getRetentionUntil();
        if (retentionUntil == null || Instant.now().isAfter(retentionUntil)) {
            throw new ConflictException("Organization is outside the export retention window.");
        }
    }

    private OrganizationExportResponse toResponse(OrganizationEntity organization) {
        Instant now = Instant.now();
        boolean eligible = organization.getLifecycleState() == OrganizationLifecycleState.OFFBOARDED
                && organization.getRetentionUntil() != null
                && !now.isAfter(organization.getRetentionUntil())
                && organization.getDataExportStatus() != OrganizationDataExportStatus.EXPORTED
                && organization.getDataExportStatus() != OrganizationDataExportStatus.NOT_REQUIRED;
        return new OrganizationExportResponse(
                organization.getId(),
                organization.getLifecycleState(),
                organization.getDataExportStatus(),
                eligible,
                organization.getOffboardedAt(),
                organization.getRetentionUntil(),
                organization.getDataExportedAt(),
                organization.getUpdatedAt());
    }

    private void recordAudit(AuthenticatedUser actor, String targetTenantId, String eventType, String organizationId, String metadataJson) {
        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actor.userId() != null ? actor.userId() : actor.subject(),
                primaryRole(actor),
                actor.tenantId(),
                targetTenantId,
                "ORGANIZATION",
                organizationId,
                null,
                metadataJson,
                actor.hasRole(Role.SUPPORT)
                        && (actor.activeTenantId() == null || !targetTenantId.equals(actor.activeTenantId())),
                null,
                AppModule.TENANT.name(),
                Instant.now()));
    }

    private Role primaryRole(AuthenticatedUser actor) {
        List<Role> precedence = List.of(Role.ADMIN, Role.SUPPORT, Role.MANAGER, Role.AUDITOR, Role.MEMBER);
        return precedence.stream()
                .filter(actor.roles()::contains)
                .findFirst()
                .orElse(Role.MEMBER);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
