package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.tenant.TenantUserPurgePort;
import com.oryzem.programmanagementsystem.platform.tenant.TenantUserQueryPort;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class TenantUserPortAdapter implements TenantUserQueryPort, TenantUserPurgePort {

    private final UserRepository userRepository;
    private final UserIdentityGateway userIdentityGateway;
    private final AccessContextService accessContextService;

    TenantUserPortAdapter(
            UserRepository userRepository,
            UserIdentityGateway userIdentityGateway,
            AccessContextService accessContextService) {
        this.userRepository = userRepository;
        this.userIdentityGateway = userIdentityGateway;
        this.accessContextService = accessContextService;
    }

    @Override
    public boolean hasInvitedOrActiveAdmin(String organizationId) {
        return userRepository.hasInvitedOrActiveAdmin(organizationId);
    }

    @Override
    public boolean hasInvitedOrActiveUsers(String organizationId) {
        Set<String> userIdsInOrganization = accessContextService.findUserIdsByOrganization(organizationId);
        return userRepository.findAll().stream()
                .filter(user -> userIdsInOrganization.contains(user.id()))
                .anyMatch(user -> user.status() != UserStatus.INACTIVE);
    }

    @Override
    public Map<String, OrganizationUserStats> summarizeByOrganizationIds(Set<String> organizationIds) {
        return userRepository.findAll().stream()
                .filter(user -> accessContextService.resolvePrimaryOrganizationId(user)
                        .filter(organizationIds::contains)
                        .isPresent())
                .collect(Collectors.groupingBy(
                        user -> accessContextService.resolvePrimaryOrganizationId(user).orElse("unknown"),
                        Collectors.collectingAndThen(Collectors.toList(), this::toStats)));
    }

    @Override
    @Transactional
    public int purgeUsersByOrganizationIds(Set<String> organizationIds) {
        AccessContextService.MembershipPurgeResult membershipPurgeResult =
                accessContextService.deleteMembershipsByOrganizations(organizationIds);

        java.util.List<ManagedUser> usersToPurge = userRepository.findAll().stream()
                .filter(user -> membershipPurgeResult.affectedUserIds().contains(user.id()))
                .filter(user -> !accessContextService.hasMemberships(user.id()))
                .toList();

        for (ManagedUser user : usersToPurge) {
            userIdentityGateway.deleteUser(user);
            userRepository.deleteById(user.id());
        }
        return usersToPurge.size();
    }

    @Override
    @Transactional
    public OffboardingSummary offboardUsersByOrganizationIds(Set<String> organizationIds, java.time.Instant retentionUntil) {
        AccessContextService.MembershipOffboardingResult membershipResult =
                accessContextService.offboardMembershipsByOrganizations(organizationIds, retentionUntil);

        int disabledUsers = 0;
        for (ManagedUser user : userRepository.findAll().stream()
                .filter(candidate -> membershipResult.affectedUserIds().contains(candidate.id()))
                .toList()) {
            if (accessContextService.hasActiveMemberships(user.id())) {
                continue;
            }
            if (user.status() != UserStatus.INACTIVE) {
                ManagedUser disabledUser = userRepository.save(user.withStatus(UserStatus.INACTIVE));
                userIdentityGateway.disableUser(disabledUser);
                disabledUsers++;
            }
        }

        return new OffboardingSummary(
                membershipResult.affectedUserIds().size(),
                disabledUsers,
                membershipResult.offboardedMemberships());
    }

    private OrganizationUserStats toStats(java.util.List<ManagedUser> users) {
        long invitedCount = users.stream().filter(user -> user.status() == UserStatus.INVITED).count();
        long activeCount = users.stream().filter(user -> user.status() == UserStatus.ACTIVE).count();
        long inactiveCount = users.stream().filter(user -> user.status() == UserStatus.INACTIVE).count();
        boolean hasInvitedOrActiveAdmin = users.stream()
                .anyMatch(user -> accessContextService.resolvePrimaryRole(user)
                                .orElse(null) == com.oryzem.programmanagementsystem.platform.authorization.Role.ADMIN
                        && user.status() != UserStatus.INACTIVE);
        return new OrganizationUserStats(
                Math.toIntExact(invitedCount),
                Math.toIntExact(activeCount),
                Math.toIntExact(inactiveCount),
                hasInvitedOrActiveAdmin);
    }
}
