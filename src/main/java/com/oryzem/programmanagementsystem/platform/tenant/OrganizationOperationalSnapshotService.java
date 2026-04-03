package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationOperationalSnapshotService {

    private final OrganizationRepository organizationRepository;

    public OrganizationOperationalSnapshotService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<OrganizationOperationalSnapshot> findAllSnapshots() {
        Map<String, List<OrganizationEntity>> organizationsByTenantId = organizationRepository.findAll().stream()
                .collect(Collectors.groupingBy(OrganizationEntity::getTenantId, LinkedHashMap::new, Collectors.toList()));

        return organizationsByTenantId.entrySet().stream()
                .map(entry -> toSnapshot(entry.getKey(), entry.getValue()))
                .toList();
    }

    private OrganizationOperationalSnapshot toSnapshot(String tenantId, List<OrganizationEntity> organizations) {
        long organizationCount = organizations.size();
        long activeOrganizationCount = organizations.stream()
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .count();
        long inactiveOrganizationCount = organizations.stream()
                .filter(organization -> organization.getStatus() == OrganizationStatus.INACTIVE)
                .count();
        long offboardingOrganizationCount = organizations.stream()
                .filter(organization -> organization.getLifecycleState() == OrganizationLifecycleState.OFFBOARDING)
                .count();
        long offboardedOrganizationCount = organizations.stream()
                .filter(organization -> organization.getLifecycleState() == OrganizationLifecycleState.OFFBOARDED)
                .count();
        long purgedOrganizationCount = organizations.stream()
                .filter(organization -> organization.getLifecycleState() == OrganizationLifecycleState.PURGED)
                .count();
        long notRequestedExportCount = organizations.stream()
                .filter(organization -> organization.getDataExportStatus() == OrganizationDataExportStatus.NOT_REQUESTED)
                .count();
        long readyForExportCount = organizations.stream()
                .filter(organization -> organization.getDataExportStatus() == OrganizationDataExportStatus.READY_FOR_EXPORT)
                .count();
        long exportInProgressCount = organizations.stream()
                .filter(organization -> organization.getDataExportStatus() == OrganizationDataExportStatus.EXPORT_IN_PROGRESS)
                .count();
        long exportedCount = organizations.stream()
                .filter(organization -> organization.getDataExportStatus() == OrganizationDataExportStatus.EXPORTED)
                .count();

        return new OrganizationOperationalSnapshot(
                tenantId,
                organizationCount,
                activeOrganizationCount,
                inactiveOrganizationCount,
                offboardingOrganizationCount,
                offboardedOrganizationCount,
                purgedOrganizationCount,
                notRequestedExportCount,
                readyForExportCount,
                exportInProgressCount,
                exportedCount);
    }
}
