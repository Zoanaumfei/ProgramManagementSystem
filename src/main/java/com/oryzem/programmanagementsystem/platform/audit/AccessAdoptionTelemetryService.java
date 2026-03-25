package com.oryzem.programmanagementsystem.platform.audit;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AccessAdoptionTelemetryService {

    public static final String LEGACY_USERS_API_USAGE_EVENT = "LEGACY_USERS_API_USAGE";
    public static final String MEMBERSHIP_USERS_API_USAGE_EVENT = "MEMBERSHIP_USERS_API_USAGE";
    public static final String LEGACY_USERS_API_BLOCKED_EVENT = "LEGACY_USERS_API_BLOCKED";

    private static final String LEGACY_USERS_API_FAMILY = "legacy_users";
    private static final String MEMBERSHIP_USERS_API_FAMILY = "membership_users";
    private static final String METRIC_NAME = "pms.access.adoption.api.usage";
    private static final String BLOCKED_METRIC_NAME = "pms.access.adoption.api.blocked";

    private final MeterRegistry meterRegistry;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public AccessAdoptionTelemetryService(
            MeterRegistry meterRegistry,
            AuditTrailService auditTrailService,
            ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLegacyUsersUsage(
            AuthenticatedUser actor,
            String operation,
            String targetTenantId,
            String targetResourceId) {
        recordUsage(LEGACY_USERS_API_FAMILY, LEGACY_USERS_API_USAGE_EVENT, actor, operation, targetTenantId, targetResourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMembershipUsersUsage(
            AuthenticatedUser actor,
            String operation,
            String targetTenantId,
            String targetResourceId) {
        recordUsage(MEMBERSHIP_USERS_API_FAMILY, MEMBERSHIP_USERS_API_USAGE_EVENT, actor, operation, targetTenantId, targetResourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLegacyUsersBlocked(
            AuthenticatedUser actor,
            String operation,
            String targetTenantId,
            String reason) {
        Role primaryRole = primaryRole(actor);
        String effectiveTargetTenantId = hasText(targetTenantId) ? targetTenantId : actor != null ? actor.tenantId() : null;
        meterRegistry.counter(
                        BLOCKED_METRIC_NAME,
                        Tags.of(
                                "api_family", LEGACY_USERS_API_FAMILY,
                                "operation", normalize(operation),
                                "tenant", normalize(effectiveTargetTenantId),
                                "actor_role", primaryRole.name().toLowerCase(Locale.ROOT),
                                "reason", normalize(reason)))
                .increment();
        auditTrailService.record(new AuditTrailEvent(
                null,
                LEGACY_USERS_API_BLOCKED_EVENT,
                actorUserId(actor),
                primaryRole,
                actor != null ? actor.tenantId() : null,
                effectiveTargetTenantId,
                LEGACY_USERS_API_FAMILY,
                null,
                null,
                toMetadata(Map.of(
                        "apiFamily", LEGACY_USERS_API_FAMILY,
                        "operation", normalize(operation),
                        "reason", normalize(reason),
                        "status", "blocked")),
                isCrossTenant(actor, effectiveTargetTenantId),
                null,
                "ACCESS_ADOPTION",
                Instant.now()));
    }

    private void recordUsage(
            String apiFamily,
            String eventType,
            AuthenticatedUser actor,
            String operation,
            String targetTenantId,
            String targetResourceId) {
        Role primaryRole = primaryRole(actor);
        String effectiveTargetTenantId = hasText(targetTenantId) ? targetTenantId : actor != null ? actor.tenantId() : null;
        meterRegistry.counter(
                        METRIC_NAME,
                        Tags.of(
                                "api_family", apiFamily,
                                "operation", normalize(operation),
                                "tenant", normalize(effectiveTargetTenantId),
                                "actor_role", primaryRole.name().toLowerCase(Locale.ROOT)))
                .increment();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("apiFamily", apiFamily);
        metadata.put("operation", normalize(operation));
        metadata.put("actorTenantId", actor != null ? actor.tenantId() : null);
        metadata.put("targetTenantId", effectiveTargetTenantId);
        metadata.put("activeMembershipId", actor != null ? actor.membershipId() : null);

        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actorUserId(actor),
                primaryRole,
                actor != null ? actor.tenantId() : null,
                effectiveTargetTenantId,
                apiFamily,
                targetResourceId,
                null,
                toMetadata(metadata),
                isCrossTenant(actor, effectiveTargetTenantId),
                null,
                "ACCESS_ADOPTION",
                Instant.now()));
    }

    private String actorUserId(AuthenticatedUser actor) {
        if (actor == null) {
            return "anonymous";
        }
        if (hasText(actor.userId())) {
            return actor.userId();
        }
        if (hasText(actor.subject())) {
            return actor.subject();
        }
        return "unknown";
    }

    private Role primaryRole(AuthenticatedUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().isEmpty()) {
            return Role.MEMBER;
        }
        List<Role> precedence = List.of(Role.ADMIN, Role.SUPPORT, Role.MANAGER, Role.AUDITOR, Role.MEMBER);
        return precedence.stream()
                .filter(actor.roles()::contains)
                .findFirst()
                .orElse(actor.roles().stream().sorted().findFirst().orElse(Role.MEMBER));
    }

    private boolean isCrossTenant(AuthenticatedUser actor, String targetTenantId) {
        return actor != null
                && hasText(actor.tenantId())
                && hasText(targetTenantId)
                && !actor.tenantId().equals(targetTenantId);
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "none";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            return "{\"serialization\":\"failed\"}";
        }
    }
}
