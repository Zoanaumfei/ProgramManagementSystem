package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

    List<OrganizationEntity> findAllByOrderByNameAsc();

    List<OrganizationEntity> findAllByTenantIdOrderByNameAsc(String tenantId);

    boolean existsByMarketId(String marketId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}
