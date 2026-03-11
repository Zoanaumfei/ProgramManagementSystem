package com.oryzem.programmanagementsystem.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOperationJpaRepository extends JpaRepository<OperationEntity, String> {

    List<OperationEntity> findByTenantIdOrderByCreatedAtAscIdAsc(String tenantId);
}
