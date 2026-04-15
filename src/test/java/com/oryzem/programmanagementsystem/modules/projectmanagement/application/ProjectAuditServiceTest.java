package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailEvent;
import com.oryzem.programmanagementsystem.platform.audit.AuditTrailService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProjectAuditServiceTest {

    private final AuditTrailService auditTrailService = mock(AuditTrailService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-11T17:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldWrapAuditMetadataInControlledPayload() throws Exception {
        ProjectAuditService service = new ProjectAuditService(auditTrailService, objectMapper, clock);

        service.record(
                actor("tenant-a", "org-lead"),
                "PROJECT_ACCESS_DENIED",
                "tenant-a",
                "PRJ-1",
                "PROJECT",
                Map.of("permission", "VIEW_PROJECT", "result", "DENIED"));

        ArgumentCaptor<AuditTrailEvent> eventCaptor = ArgumentCaptor.forClass(AuditTrailEvent.class);
        verify(auditTrailService).record(eventCaptor.capture());

        AuditTrailEvent event = eventCaptor.getValue();
        ProjectAuditPayload payload = objectMapper.readValue(event.metadataJson(), ProjectAuditPayload.class);

        assertThat(event.createdAt()).isEqualTo(Instant.parse("2026-04-11T17:00:00Z"));
        assertThat(payload.schemaVersion()).isEqualTo(1);
        assertThat(payload.activeOrganizationId()).isEqualTo("org-lead");
        assertThat(payload.attributes()).containsEntry("permission", "VIEW_PROJECT");
        assertThat(payload.attributes()).containsEntry("result", "DENIED");
    }

    @Test
    void shouldReplaceOversizedMetadataWithTruncatedPayload() throws Exception {
        ProjectAuditService service = new ProjectAuditService(auditTrailService, objectMapper, clock);

        service.record(
                actor("tenant-a", "org-lead"),
                "PROJECT_ACCESS_DENIED",
                "tenant-a",
                "PRJ-1",
                "PROJECT",
                Map.of("blob", "x".repeat(ProjectAuditService.MAX_METADATA_CHARS)));

        ArgumentCaptor<AuditTrailEvent> eventCaptor = ArgumentCaptor.forClass(AuditTrailEvent.class);
        verify(auditTrailService).record(eventCaptor.capture());

        ProjectAuditPayload payload = objectMapper.readValue(eventCaptor.getValue().metadataJson(), ProjectAuditPayload.class);
        assertThat(payload.attributes()).containsEntry("truncated", true);
        assertThat(payload.attributes()).containsEntry("originalEntryCount", 1);
        assertThat(payload.attributes().get("keys")).isEqualTo("blob");
    }

    private AuthenticatedUser actor(String tenantId, String organizationId) {
        return new AuthenticatedUser(
                "sub-user",
                "user",
                Set.of(Role.MEMBER),
                Set.of(),
                "USR-1",
                "MBR-1",
                tenantId,
                organizationId,
                null,
                TenantType.EXTERNAL);
    }
}
