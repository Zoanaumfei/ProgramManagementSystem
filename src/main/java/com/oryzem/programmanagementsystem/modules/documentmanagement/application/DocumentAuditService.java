package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DocumentAuditService {

    private static final String SOURCE_MODULE = "DOCUMENT_MANAGEMENT";

    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public DocumentAuditService(AuditTrailService auditTrailService, ObjectMapper objectMapper) {
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    public void record(
            AuthenticatedUser actor,
            String eventType,
            String targetTenantId,
            String documentId,
            DocumentContextType contextType,
            String contextId,
            String result,
            Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("result", result);
        if (contextType != null) {
            metadata.put("contextType", contextType.name());
        }
        if (contextId != null) {
            metadata.put("contextId", contextId);
        }
        if (actor != null && actor.organizationId() != null) {
            metadata.put("activeOrganizationId", actor.organizationId());
        }
        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            metadata.putAll(extraMetadata);
        }

        auditTrailService.record(new AuditTrailEvent(
                null,
                eventType,
                actor != null ? actor.userId() : "system",
                primaryRole(actor),
                actor != null ? actor.tenantId() : targetTenantId,
                targetTenantId,
                "DOCUMENT",
                documentId,
                null,
                toJson(metadata),
                actor != null
                        && actor.tenantId() != null
                        && targetTenantId != null
                        && !actor.tenantId().equals(targetTenantId),
                null,
                SOURCE_MODULE,
                Instant.now()));
    }

    public void recordSystem(
            String eventType,
            String targetTenantId,
            String documentId,
            DocumentContextType contextType,
            String contextId,
            String result,
            Map<String, Object> extraMetadata) {
        record(null, eventType, targetTenantId, documentId, contextType, contextId, result, extraMetadata);
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize document audit metadata.", exception);
        }
    }

    private Role primaryRole(AuthenticatedUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().isEmpty()) {
            return Role.MEMBER;
        }
        return actor.roles().stream()
                .sorted(Comparator.comparingInt(this::precedence))
                .findFirst()
                .orElse(Role.MEMBER);
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