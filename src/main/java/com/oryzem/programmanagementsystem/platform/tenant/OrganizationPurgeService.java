package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationPurgeService {

    private final OrganizationRepository organizationRepository;
    private final AuthorizationService authorizationService;
    private final AuditTrailService auditTrailService;
    private final OrganizationAccessService accessService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final TenantUserPurgePort tenantUserPurgePort;
    private final TenantProjectPortfolioPort tenantProjectPortfolioPort;

    OrganizationPurgeService(
            OrganizationRepository organizationRepository,
            AuthorizationService authorizationService,
            AuditTrailService auditTrailService,
            OrganizationAccessService accessService,
            OrganizationDirectoryService organizationDirectoryService,
            TenantUserPurgePort tenantUserPurgePort,
            TenantProjectPortfolioPort tenantProjectPortfolioPort) {
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
        this.auditTrailService = auditTrailService;
        this.accessService = accessService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.tenantUserPurgePort = tenantUserPurgePort;
        this.tenantProjectPortfolioPort = tenantProjectPortfolioPort;
    }

    OrganizationPurgeResponse purgeOrganizationSubtree(
            String organizationId,
            AuthenticatedUser actor,
            boolean supportOverride,
            String justification) {
        OrganizationEntity organization = accessService.findPortfolioOrganization(organizationId);
        accessService.ensureSupportInternalPurgeActor(actor);
        ensureOrganizationPurgeExplicitlyConfirmed(supportOverride, justification);

        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, Action.PURGE)
                        .resourceTenantId(organization.getId())
                        .resourceTenantType(organization.getTenantType())
                        .auditTrailEnabled(true)
                        .supportOverride(supportOverride)
                        .justification(justification)
                        .build());
        accessService.assertAllowed(decision);

        Set<String> subtreeOrganizationIds = organizationDirectoryService.collectSubtreeIds(organization.getId());
        List<TenantProjectPortfolioPort.ProgramReference> programReferences =
                tenantProjectPortfolioPort.listProgramReferences();
        ensureSubtreeHasNoExternalProgramReferences(subtreeOrganizationIds, programReferences);

        int purgedUsers = tenantUserPurgePort.purgeUsersByOrganizationIds(subtreeOrganizationIds);
        TenantProjectPortfolioPort.PurgeProgramsResult purgeProgramsResult =
                tenantProjectPortfolioPort.purgeOwnedPrograms(subtreeOrganizationIds);
        List<OrganizationEntity> organizationsToPurge = organizationRepository.findAllById(subtreeOrganizationIds).stream()
                .sorted(Comparator.comparing(OrganizationEntity::getHierarchyLevel).reversed()
                        .thenComparing(OrganizationEntity::getCreatedAt).reversed())
                .toList();
        organizationRepository.deleteAll(organizationsToPurge);

        recordAudit(actor, organization.getId(), "ORGANIZATION_PURGE_SUBTREE", organization.getId(), justification, decision.crossTenant());
        return new OrganizationPurgeResponse(
                organization.getId(),
                Action.PURGE.name(),
                Instant.now(),
                "OK",
                organizationsToPurge.size(),
                purgeProgramsResult.purgedPrograms(),
                purgedUsers,
                purgeProgramsResult.purgedDocuments());
    }

    private void ensureOrganizationPurgeExplicitlyConfirmed(boolean supportOverride, String justification) {
        if (!supportOverride) {
            throw new IllegalArgumentException("Organization purge requires supportOverride=true.");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("Organization purge requires a justification.");
        }
    }

    private void ensureSubtreeHasNoExternalProgramReferences(
            Set<String> subtreeOrganizationIds,
            List<TenantProjectPortfolioPort.ProgramReference> allPrograms) {
        boolean hasExternalReferences = allPrograms.stream()
                .filter(program -> !subtreeOrganizationIds.contains(program.ownerOrganizationId()))
                .anyMatch(program -> program.participantOrganizationIds().stream().anyMatch(subtreeOrganizationIds::contains));
        if (hasExternalReferences) {
            throw new IllegalArgumentException(
                    "Organization subtree cannot be purged while it still participates in programs owned outside the subtree.");
        }
    }

    private void recordAudit(
            AuthenticatedUser actor,
            String targetTenantId,
            String eventType,
            String targetResourceId,
            String justification,
            boolean crossTenant) {
        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actor.subject(),
                highestRole(actor),
                actor.tenantId(),
                targetTenantId,
                "ORGANIZATION",
                targetResourceId,
                justification,
                "{\"crossTenant\":" + crossTenant + "}",
                crossTenant,
                null,
                AppModule.TENANT.name(),
                null));
    }

    private Role highestRole(AuthenticatedUser actor) {
        return actor.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .findFirst()
                .orElse(Role.MEMBER);
    }
}
