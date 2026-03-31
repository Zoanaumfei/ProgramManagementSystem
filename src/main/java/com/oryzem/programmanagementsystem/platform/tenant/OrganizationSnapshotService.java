package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OrganizationSnapshotService {
    private final OrganizationDirectoryService organizationDirectoryService;
    private final TenantUserQueryPort tenantUserQueryPort;

    OrganizationSnapshotService(
            OrganizationDirectoryService organizationDirectoryService,
            TenantUserQueryPort tenantUserQueryPort) {
        this.organizationDirectoryService = organizationDirectoryService;
        this.tenantUserQueryPort = tenantUserQueryPort;
    }

    List<OrganizationResponse> toResponses(List<OrganizationEntity> organizations) {
        OrganizationManagementSnapshot snapshot = buildSnapshot(organizations);
        return organizations.stream()
                .map(organization -> toResponse(organization, snapshot))
                .toList();
    }

    OrganizationResponse toResponse(OrganizationEntity organization) {
        return toResponse(organization, false);
    }

    OrganizationResponse toResponse(OrganizationEntity organization, boolean reused) {
        return toResponse(organization, buildSnapshot(List.of(organization)), reused);
    }

    private OrganizationResponse toResponse(
            OrganizationEntity organization,
            OrganizationManagementSnapshot snapshot) {
        return toResponse(organization, snapshot, false);
    }

    private OrganizationResponse toResponse(
            OrganizationEntity organization,
            OrganizationManagementSnapshot snapshot,
            boolean reused) {
        String organizationId = organization.getId();
        OrganizationSetupStatus setupStatus = snapshot.setupStatuses()
                .getOrDefault(organizationId, OrganizationSetupStatus.INCOMPLETED);
        OrganizationUserSummaryResponse userSummary = snapshot.userSummaries()
                .getOrDefault(organizationId, OrganizationUserSummaryResponse.empty());
        boolean canInactivate = organization.getStatus() == OrganizationStatus.ACTIVE;
        String inactivationBlockedReason = null;
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            if (organization.getLifecycleState() == OrganizationLifecycleState.OFFBOARDED) {
                inactivationBlockedReason = "Organization is already offboarded and awaiting retention or purge handling.";
            } else {
                inactivationBlockedReason = "Organization is already inactive.";
            }
        }

        long childrenCount = snapshot.childrenCountByOrganizationId()
                .getOrDefault(organizationId, organizationDirectoryService.countDirectChildren(organizationId));

        return OrganizationResponse.from(
                organization,
                (int) childrenCount,
                setupStatus,
                userSummary,
                canInactivate,
                inactivationBlockedReason,
                reused);
    }

    private OrganizationManagementSnapshot buildSnapshot(List<OrganizationEntity> organizations) {
        Set<String> organizationIds = organizations.stream()
                .map(OrganizationEntity::getId)
                .collect(Collectors.toSet());

        Map<String, TenantUserQueryPort.OrganizationUserStats> userStatsByOrganization =
                tenantUserQueryPort.summarizeByOrganizationIds(organizationIds);

        Map<String, OrganizationSetupStatus> setupStatuses = new HashMap<>();
        Map<String, OrganizationUserSummaryResponse> userSummaries = new HashMap<>();
        Map<String, Long> childrenCountByOrganizationId = new HashMap<>();
        for (String organizationId : organizationIds) {
            TenantUserQueryPort.OrganizationUserStats userStats = userStatsByOrganization.getOrDefault(
                    organizationId,
                    new TenantUserQueryPort.OrganizationUserStats(0, 0, 0, false));

            setupStatuses.put(
                    organizationId,
                    userStats.hasInvitedOrActiveAdmin()
                            ? OrganizationSetupStatus.COMPLETED
                            : OrganizationSetupStatus.INCOMPLETED);
            userSummaries.put(
                    organizationId,
                    new OrganizationUserSummaryResponse(
                            userStats.invitedCount(),
                            userStats.activeCount(),
                            userStats.inactiveCount(),
                            userStats.invitedCount() + userStats.activeCount() + userStats.inactiveCount()));
        }

        return new OrganizationManagementSnapshot(setupStatuses, userSummaries, childrenCountByOrganizationId);
    }

    private record OrganizationManagementSnapshot(
            Map<String, OrganizationSetupStatus> setupStatuses,
            Map<String, OrganizationUserSummaryResponse> userSummaries,
            Map<String, Long> childrenCountByOrganizationId) {
    }
}
