package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import java.util.Map;

public record ProjectAuditPayload(
        int schemaVersion,
        String activeOrganizationId,
        Map<String, Object> attributes) {
}
