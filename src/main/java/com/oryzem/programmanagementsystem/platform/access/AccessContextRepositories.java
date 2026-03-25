package com.oryzem.programmanagementsystem.platform.access;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTenantJpaRepository extends JpaRepository<TenantEntity, String> {

    Optional<TenantEntity> findByRootOrganizationId(String rootOrganizationId);

    List<TenantEntity> findAllByOrderByNameAsc();
}

interface SpringDataTenantMarketJpaRepository extends JpaRepository<TenantMarketEntity, String> {

    List<TenantMarketEntity> findAllByTenantIdOrderByNameAsc(String tenantId);

    Optional<TenantMarketEntity> findByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(String tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(String tenantId, String code, String id);
}

interface SpringDataUserMembershipJpaRepository extends JpaRepository<UserMembershipEntity, String> {

    List<UserMembershipEntity> findByUserIdOrderByDefaultMembershipDescJoinedAtAsc(String userId);

    Optional<UserMembershipEntity> findByUserIdAndDefaultMembershipTrue(String userId);

    Optional<UserMembershipEntity> findByIdAndUserId(String id, String userId);

    List<UserMembershipEntity> findAllByOrganizationId(String organizationId);

    List<UserMembershipEntity> findAllByOrganizationIdIn(Collection<String> organizationIds);

    List<UserMembershipEntity> findAllByTenantId(String tenantId);

    List<UserMembershipEntity> findAllByMarketIdAndStatus(String marketId, MembershipStatus status);

    void deleteByUserId(String userId);
}

interface SpringDataMembershipRoleJpaRepository extends JpaRepository<MembershipRoleEntity, String> {

    List<MembershipRoleEntity> findByMembershipId(String membershipId);

    void deleteByMembershipId(String membershipId);
}

interface SpringDataRolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, String> {

    List<RolePermissionEntity> findByRoleCodeIn(Collection<String> roleCodes);
}
