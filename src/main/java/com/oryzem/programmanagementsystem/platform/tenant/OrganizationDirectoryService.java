package com.oryzem.programmanagementsystem.platform.tenant;

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
public class OrganizationDirectoryService implements OrganizationLookup, OrganizationBootstrapPort {

    private final OrganizationRepository organizationRepository;
    private final TenantUserQueryPort tenantUserQueryPort;

    public OrganizationDirectoryService(
            OrganizationRepository organizationRepository,
            TenantUserQueryPort tenantUserQueryPort) {
        this.organizationRepository = organizationRepository;
        this.tenantUserQueryPort = tenantUserQueryPort;
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
            if (tenantType == TenantType.INTERNAL) {
                organization = OrganizationEntity.createRootInternal(
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            } else if (parentOrganizationId != null && !parentOrganizationId.isBlank()) {
                OrganizationEntity parentOrganization = organizationRepository.findById(parentOrganizationId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + parentOrganizationId));
                organization = OrganizationEntity.createChild(
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE,
                        parentOrganization);
            } else {
                organization = OrganizationEntity.createRootExternal(
                        actor,
                        name.trim(),
                        code.trim().toUpperCase(),
                        active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            }

            organization.setId(organizationId);
            if (organization.getTenantType() == TenantType.EXTERNAL
                    && organization.getParentOrganization() == null
                    && organization.getCustomerOrganization() == organization) {
                organization.setCustomerOrganization(organization);
            }
            return toEntry(organizationRepository.save(organization));
        });
    }

    private OrganizationView toEntry(OrganizationEntity organization) {
        return new OrganizationView(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getTenantType(),
                organization.getStatus() == OrganizationStatus.ACTIVE,
                organization.getParentOrganization() != null ? organization.getParentOrganization().getId() : null,
                organization.getCustomerOrganization() != null ? organization.getCustomerOrganization().getId() : null,
                organization.getHierarchyLevel());
    }
}
