package com.oryzem.programmanagementsystem.platform.access;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccessContextResetService {

    private final SpringDataMembershipRoleJpaRepository membershipRoleRepository;
    private final SpringDataUserMembershipJpaRepository membershipRepository;
    private final SpringDataTenantMarketJpaRepository tenantMarketRepository;
    private final SpringDataTenantJpaRepository tenantRepository;

    public AccessContextResetService(
            SpringDataMembershipRoleJpaRepository membershipRoleRepository,
            SpringDataUserMembershipJpaRepository membershipRepository,
            SpringDataTenantMarketJpaRepository tenantMarketRepository,
            SpringDataTenantJpaRepository tenantRepository) {
        this.membershipRoleRepository = membershipRoleRepository;
        this.membershipRepository = membershipRepository;
        this.tenantMarketRepository = tenantMarketRepository;
        this.tenantRepository = tenantRepository;
    }

    public void clearTenantStructures() {
        membershipRoleRepository.deleteAll();
        membershipRepository.deleteAll();
        tenantMarketRepository.deleteAll();
        tenantRepository.deleteAll();
    }
}
