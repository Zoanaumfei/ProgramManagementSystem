package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TenantOperationalSnapshotService {

    private final SpringDataTenantJpaRepository tenantRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final UserRepository userRepository;

    public TenantOperationalSnapshotService(
            SpringDataTenantJpaRepository tenantRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public List<TenantOperationalSnapshot> findAllSnapshots() {
        List<TenantEntity> tenants = tenantRepository.findAllByOrderByNameAsc();
        List<TenantMarketEntity> markets = tenantMarketRepository.findAll();
        List<UserMembershipEntity> memberships = membershipRepository.findAll();
        List<ManagedUser> users = userRepository.findAll();

        Map<String, List<TenantMarketEntity>> marketsByTenantId = markets.stream()
                .collect(Collectors.groupingBy(TenantMarketEntity::getTenantId, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<UserMembershipEntity>> membershipsByTenantId = memberships.stream()
                .collect(Collectors.groupingBy(UserMembershipEntity::getTenantId, LinkedHashMap::new, Collectors.toList()));
        Map<String, ManagedUser> usersById = users.stream()
                .collect(Collectors.toMap(ManagedUser::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return tenants.stream()
                .map(tenant -> toSnapshot(
                        tenant,
                        marketsByTenantId.getOrDefault(tenant.getId(), List.of()),
                        membershipsByTenantId.getOrDefault(tenant.getId(), List.of()),
                        usersById))
                .toList();
    }

    private TenantOperationalSnapshot toSnapshot(
            TenantEntity tenant,
            List<TenantMarketEntity> markets,
            List<UserMembershipEntity> memberships,
            Map<String, ManagedUser> usersById) {
        long marketCount = markets.size();
        long activeMarketCount = markets.stream()
                .filter(market -> market.getStatus() == MarketStatus.ACTIVE)
                .count();
        long membershipCount = memberships.size();
        long activeMembershipCount = memberships.stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .count();

        Set<String> memberUserIds = memberships.stream()
                .map(UserMembershipEntity::getUserId)
                .collect(Collectors.toSet());
        Set<String> activeMemberUserIds = memberships.stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .map(UserMembershipEntity::getUserId)
                .collect(Collectors.toSet());

        long userCount = memberUserIds.size();
        long activeUserCount = activeMemberUserIds.stream()
                .map(usersById::get)
                .filter(user -> user != null && user.status() == UserStatus.ACTIVE)
                .count();

        return new TenantOperationalSnapshot(
                tenant.getId(),
                tenant.getName(),
                tenant.getCode(),
                tenant.getStatus().name(),
                tenant.getServiceTier().name(),
                tenant.getTenantType().name(),
                tenant.getRootOrganizationId(),
                tenant.getDataRegion(),
                marketCount,
                activeMarketCount,
                membershipCount,
                activeMembershipCount,
                userCount,
                activeUserCount);
    }
}
