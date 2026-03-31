package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.access.TenantProvisioningService;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationDirectoryService implements OrganizationLookup, OrganizationBootstrapPort, OrganizationResetPort {

    private final OrganizationRepository organizationRepository;
    private final TenantUserQueryPort tenantUserQueryPort;
    private final TenantProvisioningService tenantProvisioningService;
    private final OrganizationRelationshipRepository relationshipRepository;

    public OrganizationDirectoryService(
            OrganizationRepository organizationRepository,
            TenantUserQueryPort tenantUserQueryPort,
            TenantProvisioningService tenantProvisioningService,
            OrganizationRelationshipRepository relationshipRepository) {
        this.organizationRepository = organizationRepository;
        this.tenantUserQueryPort = tenantUserQueryPort;
        this.tenantProvisioningService = tenantProvisioningService;
        this.relationshipRepository = relationshipRepository;
    }

    @Override
    public Optional<OrganizationView> findById(String organizationId) {
        return organizationRepository.findById(organizationId).map(this::toEntry);
    }

    @Override
    public List<OrganizationView> findAll() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    public Set<String> collectSubtreeIds(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return Set.of();
        }
        Map<String, List<String>> childrenByParentId = new java.util.HashMap<>();
        for (OrganizationRelationshipEntity relationship : relationshipRepository.findAllByRelationshipTypeAndStatus(
                OrganizationRelationshipType.CUSTOMER_SUPPLIER,
                OrganizationRelationshipStatus.ACTIVE)) {
            childrenByParentId
                    .computeIfAbsent(relationship.getSourceOrganizationId(), ignored -> new java.util.ArrayList<>())
                    .add(relationship.getTargetOrganizationId());
        }

        LinkedHashSet<String> visibleIds = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(organizationId);
        while (!stack.isEmpty()) {
            String currentOrganizationId = stack.pop();
            if (!visibleIds.add(currentOrganizationId)) {
                continue;
            }

            for (String childId : childrenByParentId.getOrDefault(currentOrganizationId, List.of())) {
                stack.push(childId);
            }
        }
        return visibleIds;
    }

    @Override
    public boolean isSameOrDescendant(String ancestorOrganizationId, String targetOrganizationId) {
        return collectSubtreeIds(ancestorOrganizationId).contains(targetOrganizationId);
    }

    @Override
    public long countDirectChildren(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return 0;
        }
        return relationshipRepository.findAllBySourceOrganizationIdAndRelationshipTypeAndStatus(
                organizationId,
                OrganizationRelationshipType.CUSTOMER_SUPPLIER,
                OrganizationRelationshipStatus.ACTIVE).size();
    }

    @Override
    public Set<String> collectDirectPartnerIds(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return Set.of();
        }
        return relationshipRepository.findAllBySourceOrganizationIdAndRelationshipTypeAndStatus(
                        organizationId,
                        OrganizationRelationshipType.PARTNER,
                        OrganizationRelationshipStatus.ACTIVE).stream()
                .map(OrganizationRelationshipEntity::getTargetOrganizationId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean isDirectPartner(String sourceOrganizationId, String targetOrganizationId) {
        if (sourceOrganizationId == null || sourceOrganizationId.isBlank()
                || targetOrganizationId == null || targetOrganizationId.isBlank()) {
            return false;
        }
        return collectDirectPartnerIds(sourceOrganizationId).contains(targetOrganizationId);
    }

    @Override
    public boolean isSetupComplete(String organizationId) {
        return tenantUserQueryPort.hasInvitedOrActiveAdmin(organizationId);
    }

    @Override
    @Transactional
    public OrganizationView ensureSeeded(
            String organizationId,
            String actor,
            String name,
            String code,
            String cnpj,
            TenantType tenantType,
            boolean active) {
        return findById(organizationId).orElseGet(() -> {
            OrganizationEntity organization;
            String tenantId = tenantProvisioningService.tenantIdForRootOrganization(organizationId);
            if (tenantType == TenantType.INTERNAL) {
                tenantProvisioningService.ensureTenantForRootOrganization(
                        organizationId,
                        name.trim(),
                        code.trim().toUpperCase(),
                        TenantType.INTERNAL,
                        active,
                        null,
                        null);
                organization = OrganizationEntity.createRootInternal(
                        organizationId,
                        tenantId,
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        OrganizationCnpj.normalize(cnpj),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            } else {
                tenantProvisioningService.ensureTenantForRootOrganization(
                        organizationId,
                        name.trim(),
                        code.trim().toUpperCase(),
                        TenantType.EXTERNAL,
                        active,
                        null,
                        null);
                organization = OrganizationEntity.createRootExternal(
                        organizationId,
                        tenantId,
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        OrganizationCnpj.normalize(cnpj),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            }

            OrganizationEntity saved = organizationRepository.save(organization);
            tenantProvisioningService.ensureTenantForRootOrganization(
                    saved.getId(),
                    saved.getName(),
                    saved.getCode(),
                    saved.getTenantType(),
                    saved.getStatus() == OrganizationStatus.ACTIVE,
                    saved.getCreatedAt(),
                    saved.getUpdatedAt());
            return toEntry(saved);
        });
    }

    @Override
    @Transactional
    public void clearOrganizations() {
        relationshipRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private OrganizationView toEntry(OrganizationEntity organization) {
        return new OrganizationView(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getCnpj(),
                organization.getTenantId(),
                organization.getMarketId(),
                organization.getTenantType(),
                organization.getStatus() == OrganizationStatus.ACTIVE);
    }
}
