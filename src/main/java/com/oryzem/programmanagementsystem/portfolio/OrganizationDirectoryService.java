package com.oryzem.programmanagementsystem.portfolio;

import java.util.Optional;
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

    @Transactional
    public OrganizationDirectoryEntry ensureSeeded(
            String organizationId,
            String actor,
            String name,
            String code,
            boolean active) {
        return findById(organizationId).orElseGet(() -> {
            OrganizationEntity organization = OrganizationEntity.create(
                    actor,
                    name.trim(),
                    code.trim().toUpperCase(),
                    active ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
            organization.setId(organizationId);
            return toEntry(organizationRepository.save(organization));
        });
    }

    private OrganizationDirectoryEntry toEntry(OrganizationEntity organization) {
        return new OrganizationDirectoryEntry(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getStatus() == OrganizationStatus.ACTIVE);
    }

    public record OrganizationDirectoryEntry(
            String id,
            String name,
            String code,
            boolean active) {
    }
}
