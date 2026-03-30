package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.access.TenantProvisioningService;
import com.oryzem.programmanagementsystem.platform.access.TenantGovernanceService;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationCommandService {

    private final OrganizationRepository organizationRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationAccessService accessService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final OrganizationSnapshotService snapshotService;
    private final TenantUserPurgePort tenantUserPurgePort;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantGovernanceService tenantGovernanceService;
    private final AuditTrailService auditTrailService;

    OrganizationCommandService(
            OrganizationRepository organizationRepository,
            AuthorizationService authorizationService,
            OrganizationAccessService accessService,
            OrganizationDirectoryService organizationDirectoryService,
            OrganizationSnapshotService snapshotService,
            TenantUserPurgePort tenantUserPurgePort,
            TenantProvisioningService tenantProvisioningService,
            TenantGovernanceService tenantGovernanceService,
            AuditTrailService auditTrailService) {
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.snapshotService = snapshotService;
        this.tenantUserPurgePort = tenantUserPurgePort;
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantGovernanceService = tenantGovernanceService;
        this.auditTrailService = auditTrailService;
    }

    OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        OrganizationEntity organization;
        if (actor != null && actor.tenantType() == TenantType.EXTERNAL) {
            if (!actor.hasRole(Role.ADMIN)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only admins can create supplier organizations.");
            }
            String tenantId = actor.tenantId();
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("Authenticated user does not have an active tenant context.");
            }
            OrganizationEntity managedOrganization = accessService.findManagedOrganization(actor.organizationId());
            accessService.assertCanManageOrganization(actor, managedOrganization);
            tenantGovernanceService.assertOrganizationQuotaAvailable(tenantId);
            organization = OrganizationEntity.createExternalForTenant(
                    OrganizationIds.newId("ORG"),
                    tenantId,
                    managedOrganization.getMarketId(),
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE));
        } else {
            accessService.assertAllowed(authorizationService.decide(
                    actor,
                    AuthorizationContext.builder(AppModule.TENANT, Action.CREATE).build()));
            accessService.assertCanCreateRootCustomer(actor);
            String organizationId = OrganizationIds.newId("ORG");
            String tenantId = tenantProvisioningService.tenantIdForRootOrganization(organizationId);
            tenantProvisioningService.ensureTenantForRootOrganization(
                    organizationId,
                    normalizeName(request.name()),
                    normalizedCode,
                    TenantType.EXTERNAL,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE) == OrganizationStatus.ACTIVE,
                    null,
                    null);
            organization = OrganizationEntity.createRootExternal(
                    organizationId,
                    tenantId,
                    actor.username(),
                    normalizeName(request.name()),
                    normalizedCode,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE));
        }
        OrganizationEntity saved = organizationRepository.save(organization);
        if (tenantProvisioningService.tenantIdForRootOrganization(saved.getId()).equals(saved.getTenantId())) {
            tenantProvisioningService.ensureTenantForRootOrganization(
                    saved.getId(),
                    saved.getName(),
                    saved.getCode(),
                    saved.getTenantType(),
                    saved.getStatus() == OrganizationStatus.ACTIVE,
                    saved.getCreatedAt(),
                    saved.getUpdatedAt());
        }
        recordAudit(actor, saved.getTenantId(), "ORGANIZATION_CREATE", saved.getId(), false, null);
        return snapshotService.toResponse(saved);
    }

    OrganizationResponse updateOrganization(
            String organizationId,
            UpdateOrganizationRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findManagedOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.EDIT);
        accessService.assertCanManageOrganization(actor, organization);
        accessService.ensureOrganizationIsMutable(organization);

        String normalizedCode = normalizeCode(request.code());
        if (organizationRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, organization.getId())) {
            throw new IllegalArgumentException("Organization code already exists.");
        }

        organization.updateDetails(actor.username(), normalizeName(request.name()), normalizedCode);
        OrganizationEntity saved = organizationRepository.save(organization);
        recordAudit(actor, saved.getTenantId(), "ORGANIZATION_UPDATE", saved.getId(), false, null);
        return snapshotService.toResponse(saved);
    }

    OrganizationResponse inactivateOrganization(String organizationId, AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findManagedOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.DELETE);
        accessService.assertCanManageOrganization(actor, organization);
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            return snapshotService.toResponse(organization);
        }

        Instant now = Instant.now();
        Instant retentionUntil = tenantGovernanceService.retentionDeadlineFrom(now);
        java.util.Set<String> subtreeIds = organizationDirectoryService.collectSubtreeIds(organization.getId());
        List<OrganizationEntity> subtree = organizationRepository.findAllById(subtreeIds);
        TenantUserPurgePort.OffboardingSummary offboardingSummary =
                tenantUserPurgePort.offboardUsersByOrganizationIds(subtreeIds, retentionUntil);

        for (OrganizationEntity node : subtree) {
            node.markOffboarding(actor.username(), now, retentionUntil);
            node.markOffboarded(actor.username(), now, retentionUntil);
            organizationRepository.save(node);
        }
        recordAudit(
                actor,
                organization.getTenantId(),
                "ORGANIZATION_OFFBOARD",
                organization.getId(),
                false,
                "{\"retentionUntil\":\"" + retentionUntil + "\",\"disabledUsers\":" + offboardingSummary.disabledUsers()
                        + ",\"affectedUsers\":" + offboardingSummary.affectedUsers()
                        + ",\"offboardedMemberships\":" + offboardingSummary.offboardedMemberships()
                        + ",\"exportStatus\":\"READY_FOR_EXPORT\"}");
        return snapshotService.toResponse(organizationRepository.findById(organizationId).orElseThrow());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private <T> T defaultValue(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private void recordAudit(
            AuthenticatedUser actor,
            String targetTenantId,
            String eventType,
            String organizationId,
            boolean crossTenant,
            String metadataJson) {
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
                metadataJson != null ? metadataJson : "{\"crossTenant\":" + crossTenant + "}",
                crossTenant,
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
}
