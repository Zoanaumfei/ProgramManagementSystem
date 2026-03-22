package com.oryzem.programmanagementsystem.platform.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByOrderByCreatedAtAscIdAsc();
}
