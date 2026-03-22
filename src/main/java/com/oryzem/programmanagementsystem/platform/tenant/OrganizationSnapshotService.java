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

    private final OrganizationRepository organizationRepository;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final TenantUserQueryPort tenantUserQueryPort;
    private final TenantProjectPortfolioPort tenantProjectPortfolioPort;

    OrganizationSnapshotService(
            OrganizationRepository organizationRepository,
            OrganizationDirectoryService organizationDirectoryService,
            TenantUserQueryPort tenantUserQueryPort,
            TenantProjectPortfolioPort tenantProjectPortfolioPort) {
        this.organizationRepository = organizationRepository;
        this.organizationDirectoryService = organizationDirectoryService;
        this.tenantUserQueryPort = tenantUserQueryPort;
        this.tenantProjectPortfolioPort = tenantProjectPortfolioPort;
    }

    List<OrganizationResponse> toResponses(List<OrganizationEntity> organizations) {
        OrganizationManagementSnapshot snapshot = buildSnapshot(organizations);
        return organizations.stream()
                .map(organization -> toResponse(organization, snapshot))
                .toList();
    }

    OrganizationResponse toResponse(OrganizationEntity organization) {
        return toResponse(organization, buildSnapshot(List.of(organization)));
    }

    private OrganizationResponse toResponse(
            OrganizationEntity organization,
            OrganizationManagementSnapshot snapshot) {
        String organizationId = organization.getId();
        OrganizationSetupStatus setupStatus = snapshot.setupStatuses()
                .getOrDefault(organizationId, OrganizationSetupStatus.INCOMPLETED);
        OrganizationUserSummaryResponse userSummary = snapshot.userSummaries()
                .getOrDefault(organizationId, OrganizationUserSummaryResponse.empty());
        OrganizationProgramSummaryResponse programSummary = snapshot.programSummaries()
                .getOrDefault(organizationId, OrganizationProgramSummaryResponse.empty());
        boolean hasActiveChildren = organizationRepository.findAllByParentOrganizationIdOrderByNameAsc(organizationId).stream()
                .anyMatch(child -> child.getStatus() == OrganizationStatus.ACTIVE);
        boolean hasActiveProjects = tenantProjectPortfolioPort.listProgramReferences().stream()
                .filter(program -> organizationId.equals(program.ownerOrganizationId()))
                .anyMatch(TenantProjectPortfolioPort.ProgramReference::hasActiveProjects);

        boolean canInactivate = organization.getStatus() == OrganizationStatus.ACTIVE
                && userSummary.invitedCount() == 0
                && userSummary.activeCount() == 0
                && !hasActiveChildren
                && !hasActiveProjects;
        String inactivationBlockedReason = null;
        if (organization.getStatus() == OrganizationStatus.INACTIVE) {
            inactivationBlockedReason = "Organization is already inactive.";
        } else if (userSummary.invitedCount() > 0 || userSummary.activeCount() > 0) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all invited or active users are inactivated.";
        } else if (hasActiveChildren) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all active child organizations are inactivated.";
        } else if (hasActiveProjects) {
            inactivationBlockedReason =
                    "Organization can only be inactivated after all active projects are closed or inactivated.";
        }

        long childrenCount = snapshot.childrenCountByOrganizationId()
                .getOrDefault(organizationId, organizationDirectoryService.countDirectChildren(organizationId));

        return OrganizationResponse.from(
                organization,
                (int) childrenCount,
                setupStatus,
                userSummary,
                programSummary,
                canInactivate,
                inactivationBlockedReason);
    }

    private OrganizationManagementSnapshot buildSnapshot(List<OrganizationEntity> organizations) {
        Set<String> organizationIds = organizations.stream()
                .map(OrganizationEntity::getId)
                .collect(Collectors.toSet());

        Map<String, TenantUserQueryPort.OrganizationUserStats> userStatsByOrganization =
                tenantUserQueryPort.summarizeByOrganizationIds(organizationIds);
        Map<String, ProgramCounters> programCountersByOrganization = new HashMap<>();
        for (TenantProjectPortfolioPort.ProgramReference program : tenantProjectPortfolioPort.listProgramReferences()) {
            String ownerOrganizationId = program.ownerOrganizationId();
            if (organizationIds.contains(ownerOrganizationId)) {
                programCountersByOrganization
                        .computeIfAbsent(ownerOrganizationId, ignored -> new ProgramCounters())
                        .incrementOwned();
            }

            program.participantOrganizationIds().stream()
                    .filter(organizationIds::contains)
                    .filter(participantOrganizationId -> !participantOrganizationId.equals(ownerOrganizationId))
                    .distinct()
                    .forEach(participantOrganizationId -> programCountersByOrganization
                            .computeIfAbsent(participantOrganizationId, ignored -> new ProgramCounters())
                            .incrementParticipating());
        }

        Map<String, OrganizationSetupStatus> setupStatuses = new HashMap<>();
        Map<String, OrganizationUserSummaryResponse> userSummaries = new HashMap<>();
        Map<String, OrganizationProgramSummaryResponse> programSummaries = new HashMap<>();
        Map<String, Long> childrenCountByOrganizationId = organizations.stream()
                .filter(organization -> organization.getParentOrganization() != null)
                .collect(Collectors.groupingBy(
                        organization -> organization.getParentOrganization().getId(),
                        Collectors.counting()));
        for (String organizationId : organizationIds) {
            TenantUserQueryPort.OrganizationUserStats userStats = userStatsByOrganization.getOrDefault(
                    organizationId,
                    new TenantUserQueryPort.OrganizationUserStats(0, 0, 0, false));
            ProgramCounters programCounters = programCountersByOrganization.getOrDefault(
                    organizationId,
                    new ProgramCounters());

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
            programSummaries.put(organizationId, programCounters.toResponse());
        }

        return new OrganizationManagementSnapshot(setupStatuses, userSummaries, programSummaries, childrenCountByOrganizationId);
    }

    private record OrganizationManagementSnapshot(
            Map<String, OrganizationSetupStatus> setupStatuses,
            Map<String, OrganizationUserSummaryResponse> userSummaries,
            Map<String, OrganizationProgramSummaryResponse> programSummaries,
            Map<String, Long> childrenCountByOrganizationId) {
    }

    private static final class ProgramCounters {

        private int ownedCount;
        private int participatingCount;

        void incrementOwned() {
            ownedCount++;
        }

        void incrementParticipating() {
            participatingCount++;
        }

        OrganizationProgramSummaryResponse toResponse() {
            return new OrganizationProgramSummaryResponse(
                    ownedCount,
                    participatingCount,
                    ownedCount + participatingCount);
        }
    }
}
