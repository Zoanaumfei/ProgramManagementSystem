package com.oryzem.programmanagementsystem.platform.users.infrastructure;

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

    TenantUserPortAdapter(
            UserRepository userRepository,
            UserIdentityGateway userIdentityGateway) {
        this.userRepository = userRepository;
        this.userIdentityGateway = userIdentityGateway;
    }

    @Override
    public boolean hasInvitedOrActiveAdmin(String organizationId) {
        return userRepository.hasInvitedOrActiveAdmin(organizationId);
    }

    @Override
    public boolean hasInvitedOrActiveUsers(String organizationId) {
        return userRepository.findByTenantId(organizationId).stream()
                .anyMatch(user -> user.status() != UserStatus.INACTIVE);
    }

    @Override
    public Map<String, OrganizationUserStats> summarizeByOrganizationIds(Set<String> organizationIds) {
        return userRepository.findAll().stream()
                .filter(user -> organizationIds.contains(user.tenantId()))
                .collect(Collectors.groupingBy(
                        ManagedUser::tenantId,
                        Collectors.collectingAndThen(Collectors.toList(), this::toStats)));
    }

    @Override
    @Transactional
    public int purgeUsersByOrganizationIds(Set<String> organizationIds) {
        java.util.List<ManagedUser> usersToPurge = userRepository.findAll().stream()
                .filter(user -> organizationIds.contains(user.tenantId()))
                .toList();

        for (ManagedUser user : usersToPurge) {
            userIdentityGateway.deleteUser(user);
            userRepository.deleteById(user.id());
        }
        return usersToPurge.size();
    }

    private OrganizationUserStats toStats(java.util.List<ManagedUser> users) {
        long invitedCount = users.stream().filter(user -> user.status() == UserStatus.INVITED).count();
        long activeCount = users.stream().filter(user -> user.status() == UserStatus.ACTIVE).count();
        long inactiveCount = users.stream().filter(user -> user.status() == UserStatus.INACTIVE).count();
        boolean hasInvitedOrActiveAdmin = users.stream()
                .anyMatch(user -> user.role() == com.oryzem.programmanagementsystem.platform.authorization.Role.ADMIN
                        && user.status() != UserStatus.INACTIVE);
        return new OrganizationUserStats(
                Math.toIntExact(invitedCount),
                Math.toIntExact(activeCount),
                Math.toIntExact(inactiveCount),
                hasInvitedOrActiveAdmin);
    }
}
