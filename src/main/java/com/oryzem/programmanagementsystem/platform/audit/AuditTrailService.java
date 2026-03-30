package com.oryzem.programmanagementsystem.platform.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditTrailService {

    private final SpringDataAuditLogJpaRepository repository;
    private final RequestCorrelationContext requestCorrelationContext;

    public AuditTrailService(
            SpringDataAuditLogJpaRepository repository,
            RequestCorrelationContext requestCorrelationContext) {
        this.repository = repository;
        this.requestCorrelationContext = requestCorrelationContext;
    }

    public void record(AuditTrailEvent event) {
        repository.save(AuditLogEntity.from(enrich(event)));
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEvent> findAll() {
        return repository.findAllByOrderByCreatedAtAscIdAsc().stream()
                .map(AuditLogEntity::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditTrailEvent> findByEventTypeAndTargetTenantAndResourceType(
            String eventType,
            String targetTenantId,
            String targetResourceType) {
        if (!hasText(eventType) || !hasText(targetTenantId) || !hasText(targetResourceType)) {
            return List.of();
        }
        return repository.findAllByEventTypeAndTargetTenantIdAndTargetResourceTypeOrderByCreatedAtAscIdAsc(
                        eventType,
                        targetTenantId,
                        targetResourceType).stream()
                .map(AuditLogEntity::toDomain)
                .toList();
    }

    public void clear() {
        repository.deleteAll();
    }

    private AuditTrailEvent enrich(AuditTrailEvent event) {
        return new AuditTrailEvent(
                hasText(event.id()) ? event.id() : "AUD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(),
                event.eventType(),
                event.actorUserId(),
                event.actorRole(),
                event.actorTenantId(),
                event.targetTenantId(),
                event.targetResourceType(),
                event.targetResourceId(),
                event.justification(),
                event.metadataJson(),
                event.crossTenant(),
                hasText(event.correlationId()) ? event.correlationId() : requestCorrelationContext.getOrCreate(),
                event.sourceModule(),
                event.createdAt() != null ? event.createdAt() : Instant.now());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
