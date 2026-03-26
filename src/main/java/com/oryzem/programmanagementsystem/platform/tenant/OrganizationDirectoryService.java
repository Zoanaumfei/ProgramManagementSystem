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

    public OrganizationDirectoryService(
            OrganizationRepository organizationRepository,
            TenantUserQueryPort tenantUserQueryPort,
            TenantProvisioningService tenantProvisioningService) {
        this.organizationRepository = organizationRepository;
        this.tenantUserQueryPort = tenantUserQueryPort;
        this.tenantProvisioningService = tenantProvisioningService;
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
        Map<String, List<OrganizationEntity>> childrenByParentId = new java.util.HashMap<>();
        for (OrganizationEntity organization : organizationRepository.findAllByOrderByNameAsc()) {
            if (organization.getParentOrganization() == null) {
                continue;
            }
            childrenByParentId
                    .computeIfAbsent(organization.getParentOrganization().getId(), ignored -> new java.util.ArrayList<>())
                    .add(organization);
        }

        LinkedHashSet<String> visibleIds = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(organizationId);
        while (!stack.isEmpty()) {
            String currentOrganizationId = stack.pop();
            if (!visibleIds.add(currentOrganizationId)) {
                continue;
            }

            for (OrganizationEntity child : childrenByParentId.getOrDefault(currentOrganizationId, List.of())) {
                stack.push(child.getId());
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
        return organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).size();
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
            TenantType tenantType,
            String parentOrganizationId,
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
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            } else if (parentOrganizationId != null && !parentOrganizationId.isBlank()) {
                OrganizationEntity parentOrganization = organizationRepository.findById(parentOrganizationId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + parentOrganizationId));
                organization = OrganizationEntity.createChild(
                        organizationId,
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE,
                        parentOrganization);
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
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            }

            if (organization.getTenantType() == TenantType.EXTERNAL
                    && organization.getParentOrganization() == null
                    && organization.getCustomerOrganization() == organization) {
                organization.setCustomerOrganization(organization);
            }
            OrganizationEntity saved = organizationRepository.save(organization);
            if (saved.getParentOrganization() == null) {
                tenantProvisioningService.ensureTenantForRootOrganization(
                        saved.getId(),
                        saved.getName(),
                        saved.getCode(),
                        saved.getTenantType(),
                        saved.getStatus() == OrganizationStatus.ACTIVE,
                        saved.getCreatedAt(),
                        saved.getUpdatedAt());
            }
            return toEntry(saved);
        });
    }

    @Override
    @Transactional
    public void clearOrganizations() {
        organizationRepository.deleteAll();
    }

    private OrganizationView toEntry(OrganizationEntity organization) {
        return new OrganizationView(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getTenantId(),
                organization.getMarketId(),
                organization.getTenantType(),
                organization.getStatus() == OrganizationStatus.ACTIVE,
                organization.getParentOrganization() != null ? organization.getParentOrganization().getId() : null,
                organization.getCustomerOrganization() != null ? organization.getCustomerOrganization().getId() : null,
                organization.getHierarchyLevel());
    }
}
