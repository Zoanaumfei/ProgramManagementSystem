package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

    List<OrganizationEntity> findAllByOrderByNameAsc();

    List<OrganizationEntity> findAllByTenantIdOrderByNameAsc(String tenantId);

    boolean existsByMarketId(String marketId);

    Optional<OrganizationEntity> findByTenantIdAndCnpj(String tenantId, String cnpj);

    boolean existsByTenantIdAndCnpjAndIdNot(String tenantId, String cnpj, String id);
}
