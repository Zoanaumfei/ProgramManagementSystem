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
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationCommandService {

    private final OrganizationRepository organizationRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationAccessService accessService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final OrganizationRelationshipRepository relationshipRepository;
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
            OrganizationRelationshipRepository relationshipRepository,
            OrganizationSnapshotService snapshotService,
            TenantUserPurgePort tenantUserPurgePort,
            TenantProvisioningService tenantProvisioningService,
            TenantGovernanceService tenantGovernanceService,
            AuditTrailService auditTrailService) {
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.relationshipRepository = relationshipRepository;
        this.snapshotService = snapshotService;
        this.tenantUserPurgePort = tenantUserPurgePort;
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantGovernanceService = tenantGovernanceService;
        this.auditTrailService = auditTrailService;
    }

    OrganizationResponse createOrganization(CreateOrganizationRequest request, AuthenticatedUser actor) {
        String normalizedName = normalizeName(request.name());
        String normalizedCnpj = normalizeCnpj(request.cnpj());
        String normalizedLocalOrganizationCode = normalizeLocalOrganizationCode(request.localOrganizationCode());

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
            OrganizationEntity existingOrganization = organizationRepository.findByTenantIdAndCnpj(tenantId, normalizedCnpj)
                    .orElse(null);
            if (existingOrganization != null) {
                accessService.ensureOrganizationIsMutable(existingOrganization);
                if (existingOrganization.getId().equals(managedOrganization.getId())) {
                    throw new IllegalArgumentException("The active organization already uses this CNPJ.");
                }
                ensureCustomerSupplierRelationship(
                        managedOrganization,
                        existingOrganization,
                        actor.username(),
                        normalizedLocalOrganizationCode);
                recordAudit(
                        actor,
                        existingOrganization.getTenantId(),
                        "ORGANIZATION_LINK_EXISTING",
                        existingOrganization.getId(),
                        false,
                        "{\"reused\":true}");
                return snapshotService.toResponse(existingOrganization, true);
            }
            tenantGovernanceService.assertOrganizationQuotaAvailable(tenantId);
            organization = OrganizationEntity.createExternalForTenant(
                    OrganizationIds.newId("ORG"),
                    tenantId,
                    managedOrganization.getMarketId(),
                    actor.username(),
                    normalizedName,
                    normalizedCnpj,
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
                    normalizedName,
                    tenantCodeForRootOrganization(organizationId, normalizedName, normalizedCnpj, TenantType.EXTERNAL),
                    TenantType.EXTERNAL,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE) == OrganizationStatus.ACTIVE,
                    null,
                    null);
            organization = OrganizationEntity.createRootExternal(
                    organizationId,
                    tenantId,
                    actor.username(),
                    normalizedName,
                    normalizedCnpj,
                    defaultValue(request.status(), OrganizationStatus.ACTIVE));
        }
        OrganizationEntity saved = organizationRepository.save(organization);
        if (actor != null && actor.tenantType() == TenantType.EXTERNAL) {
            OrganizationEntity sourceOrganization = accessService.findManagedOrganization(actor.organizationId());
            ensureCustomerSupplierRelationship(
                    sourceOrganization,
                    saved,
                    actor.username(),
                    normalizedLocalOrganizationCode);
        }
        if (tenantProvisioningService.tenantIdForRootOrganization(saved.getId()).equals(saved.getTenantId())) {
            tenantProvisioningService.ensureTenantForRootOrganization(
                    saved.getId(),
                    saved.getName(),
                    tenantCodeForRootOrganization(
                            saved.getId(),
                            saved.getName(),
                            saved.getCnpj(),
                            saved.getTenantType()),
                    saved.getTenantType(),
                    saved.getStatus() == OrganizationStatus.ACTIVE,
                    saved.getCreatedAt(),
                    saved.getUpdatedAt());
        }
        recordAudit(actor, saved.getTenantId(), "ORGANIZATION_CREATE", saved.getId(), false, null);
        return snapshotService.toResponse(saved, false);
    }

    OrganizationResponse updateOrganization(
            String organizationId,
            UpdateOrganizationRequest request,
            AuthenticatedUser actor) {
        OrganizationEntity organization = accessService.findManagedOrganization(organizationId);
        accessService.assertCanAccessOrganization(actor, organization, Action.EDIT);
        accessService.assertCanManageOrganization(actor, organization);
        accessService.ensureOrganizationIsMutable(organization);

        String normalizedCnpj = normalizeCnpj(request.cnpj());
        if (organizationRepository.existsByTenantIdAndCnpjAndIdNot(organization.getTenantId(), normalizedCnpj, organization.getId())) {
            throw organizationCnpjAlreadyExistsInTenant(normalizedCnpj);
        }

        organization.updateDetails(actor.username(), normalizeName(request.name()), normalizedCnpj);
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

    private String normalizeCnpj(String value) {
        return OrganizationCnpj.normalize(value);
    }

    private String normalizeLocalOrganizationCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
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

    private void ensureCustomerSupplierRelationship(
            OrganizationEntity source,
            OrganizationEntity target,
            String actor,
            String localOrganizationCode) {
        if (source.getId().equals(target.getId())) {
            return;
        }
        if (organizationDirectoryService.collectSubtreeIds(target.getId()).contains(source.getId())) {
            throw relationshipCycleNotAllowed(source.getId(), target.getId());
        }
        OrganizationRelationshipEntity relationship = relationshipRepository
                .findBySourceOrganizationIdAndTargetOrganizationIdAndRelationshipType(
                        source.getId(),
                        target.getId(),
                        OrganizationRelationshipType.CUSTOMER_SUPPLIER)
                .orElseGet(() -> OrganizationRelationshipEntity.create(
                        OrganizationIds.newId("REL"),
                        actor,
                        source.getId(),
                        target.getId(),
                        OrganizationRelationshipType.CUSTOMER_SUPPLIER,
                        localOrganizationCode,
                        OrganizationRelationshipStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()));
        validateLocalOrganizationCode(source.getId(), localOrganizationCode, relationship.getId());
        relationship.setLocalOrganizationCode(localOrganizationCode);
        if (relationship.getStatus() != OrganizationRelationshipStatus.ACTIVE) {
            relationship.setStatus(OrganizationRelationshipStatus.ACTIVE);
        }
        relationship.touch(actor);
        relationshipRepository.save(relationship);
    }

    private BusinessRuleException organizationCnpjAlreadyExistsInTenant(String cnpj) {
        return new BusinessRuleException(
                "ORGANIZATION_CNPJ_ALREADY_EXISTS_IN_TENANT",
                "Organization CNPJ already exists in this tenant.",
                fieldDetails("cnpj", cnpj));
    }

    private BusinessRuleException relationshipCycleNotAllowed(String sourceOrganizationId, String targetOrganizationId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sourceOrganizationId", sourceOrganizationId);
        details.put("targetOrganizationId", targetOrganizationId);
        return new BusinessRuleException(
                "ORGANIZATION_RELATIONSHIP_CYCLE_NOT_ALLOWED",
                "Relationship would create a cycle in the customer/supplier graph.",
                details);
    }

    private Map<String, Object> fieldDetails(String field, String value) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", field);
        details.put("value", value);
        return details;
    }

    private void validateLocalOrganizationCode(
            String sourceOrganizationId,
            String localOrganizationCode,
            String excludedRelationshipId) {
        if (localOrganizationCode == null) {
            return;
        }
        boolean exists = excludedRelationshipId == null
                ? relationshipRepository.existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCase(
                        sourceOrganizationId,
                        localOrganizationCode)
                : relationshipRepository.existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCaseAndIdNot(
                        sourceOrganizationId,
                        localOrganizationCode,
                        excludedRelationshipId);
        if (exists) {
            throw new BusinessRuleException(
                    "ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS",
                    "Local organization code already exists for this organization.",
                    fieldDetails("localOrganizationCode", localOrganizationCode));
        }
    }

    private String tenantCodeForRootOrganization(
            String organizationId,
            String organizationName,
            String cnpj,
            TenantType tenantType) {
        if (tenantType == TenantType.INTERNAL) {
            return ("INT-" + organizationId).toUpperCase(Locale.ROOT);
        }
        if (cnpj != null && !cnpj.isBlank()) {
            return ("ORG-" + cnpj).toUpperCase(Locale.ROOT);
        }
        String normalizedName = organizationName == null ? "ORG" : organizationName.trim().toUpperCase(Locale.ROOT);
        normalizedName = normalizedName.replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (normalizedName.isBlank()) {
            normalizedName = "ORG";
        }
        return (normalizedName + "-" + organizationId).substring(0, Math.min((normalizedName + "-" + organizationId).length(), 80));
    }
}
