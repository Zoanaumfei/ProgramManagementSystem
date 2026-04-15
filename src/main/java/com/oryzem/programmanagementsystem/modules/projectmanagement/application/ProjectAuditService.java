package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProjectAuditService {

    private static final String SOURCE_MODULE = "PROJECT_MANAGEMENT";
    static final int MAX_METADATA_CHARS = 8_000;

    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ProjectAuditService(AuditTrailService auditTrailService, ObjectMapper objectMapper, Clock clock) {
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(
            AuthenticatedUser actor,
            String eventType,
            String targetTenantId,
            String resourceId,
            String resourceType,
            Map<String, Object> metadata) {
        String activeOrganizationId = actor != null ? actor.organizationId() : null;
        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actor != null ? actor.userId() : "system",
                primaryRole(actor),
                actor != null ? actor.tenantId() : targetTenantId,
                targetTenantId,
                resourceType,
                resourceId,
                null,
                toJson(activeOrganizationId, metadata),
                actor != null && actor.tenantId() != null && targetTenantId != null && !actor.tenantId().equals(targetTenantId),
                null,
                SOURCE_MODULE,
                Instant.now(clock)));
    }

    private String toJson(String activeOrganizationId, Map<String, Object> metadata) {
        try {
            ProjectAuditPayload payload = new ProjectAuditPayload(
                    1,
                    activeOrganizationId,
                    normalize(metadata));
            String serializedPayload = objectMapper.writeValueAsString(payload);
            if (serializedPayload.length() <= MAX_METADATA_CHARS) {
                return serializedPayload;
            }
            ProjectAuditPayload truncatedPayload = new ProjectAuditPayload(
                    1,
                    activeOrganizationId,
                    Map.of(
                            "truncated", true,
                            "originalEntryCount", metadata != null ? metadata.size() : 0,
                            "keys", normalizeKeys(metadata)));
            return objectMapper.writeValueAsString(truncatedPayload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize project audit metadata.", exception);
        }
    }

    private Map<String, Object> normalize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    private String normalizeKeys(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        return metadata.keySet().stream()
                .sorted()
                .limit(25)
                .collect(Collectors.joining(","));
    }

    private Role primaryRole(AuthenticatedUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().isEmpty()) {
            return Role.MEMBER;
        }
        return actor.roles().stream().sorted(Comparator.comparingInt(this::precedence)).findFirst().orElse(Role.MEMBER);
    }

    private int precedence(Role role) {
        return switch (role) {
            case ADMIN -> 0;
            case SUPPORT -> 1;
            case MANAGER -> 2;
            case AUDITOR -> 3;
            case MEMBER -> 4;
        };
    }
}
