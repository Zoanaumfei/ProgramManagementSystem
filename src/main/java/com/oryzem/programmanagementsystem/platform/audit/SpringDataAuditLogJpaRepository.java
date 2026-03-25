package com.oryzem.programmanagementsystem.platform.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByOrderByCreatedAtAscIdAsc();

    List<AuditLogEntity> findAllByEventTypeInAndCreatedAtGreaterThanEqualOrderByCreatedAtAscIdAsc(
            List<String> eventTypes,
            Instant createdAt);
}
