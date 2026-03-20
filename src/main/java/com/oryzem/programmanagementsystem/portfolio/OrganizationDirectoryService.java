package com.oryzem.programmanagementsystem.portfolio;

import com.oryzem.programmanagementsystem.authorization.TenantType;
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
public class OrganizationDirectoryService {

    private final OrganizationRepository organizationRepository;

    public OrganizationDirectoryService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Optional<OrganizationDirectoryEntry> findById(String organizationId) {
        return organizationRepository.findById(organizationId).map(this::toEntry);
    }

    public OrganizationDirectoryEntry getRequired(String organizationId) {
        return findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    public List<OrganizationDirectoryEntry> findAll() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toEntry)
                .toList();
    }

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

    public boolean isSameOrDescendant(String ancestorOrganizationId, String targetOrganizationId) {
        return collectSubtreeIds(ancestorOrganizationId).contains(targetOrganizationId);
    }

    public long countDirectChildren(String organizationId) {
        return organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).size();
    }

    @Transactional
    public OrganizationDirectoryEntry ensureSeeded(
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

    private OrganizationDirectoryEntry toEntry(OrganizationEntity organization) {
        return new OrganizationDirectoryEntry(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getTenantType(),
                organization.getStatus() == OrganizationStatus.ACTIVE,
                organization.getParentOrganization() != null ? organization.getParentOrganization().getId() : null,
                organization.getCustomerOrganization() != null ? organization.getCustomerOrganization().getId() : null,
                organization.getHierarchyLevel());
    }

    public record OrganizationDirectoryEntry(
            String id,
            String name,
            String code,
            TenantType tenantType,
            boolean active,
            String parentOrganizationId,
            String customerOrganizationId,
            Integer hierarchyLevel) {
    }
}
