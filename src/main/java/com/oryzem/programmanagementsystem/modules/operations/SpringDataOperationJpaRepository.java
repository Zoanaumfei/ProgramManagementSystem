package com.oryzem.programmanagementsystem.modules.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOperationJpaRepository extends JpaRepository<OperationEntity, String> {

    List<OperationEntity> findByTenantIdOrderByCreatedAtAscIdAsc(String tenantId);

    List<OperationEntity> findByTenantIdInOrderByCreatedAtAscIdAsc(Iterable<String> tenantIds);
}
