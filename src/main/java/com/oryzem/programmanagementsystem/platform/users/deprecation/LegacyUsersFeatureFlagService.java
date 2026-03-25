package com.oryzem.programmanagementsystem.platform.users.deprecation;

import com.oryzem.programmanagementsystem.platform.audit.AccessAdoptionTelemetryService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LegacyUsersFeatureFlagService {

    public static final String LEGACY_USERS_PATH = "/api/users";
    public static final String REPLACEMENT_PATH = "/api/access/users/{userId}/memberships";

    private final LegacyUsersFeatureFlagsProperties properties;
    private final AccessAdoptionTelemetryService telemetryService;

    public LegacyUsersFeatureFlagService(
            LegacyUsersFeatureFlagsProperties properties,
            AccessAdoptionTelemetryService telemetryService) {
        this.properties = properties;
        this.telemetryService = telemetryService;
    }

    public boolean isUiEnabled() {
        return properties.isUiEnabled();
    }

    public boolean isReadEnabled() {
        return properties.isReadEnabled();
    }

    public boolean isWriteEnabled() {
        return properties.isWriteEnabled();
    }

    public LegacyUsersDeprecationStage currentStage() {
        if (!properties.isReadEnabled() && !properties.isWriteEnabled()) {
            return LegacyUsersDeprecationStage.OFF_BY_DEFAULT;
        }
        if (properties.isReadEnabled() && !properties.isWriteEnabled()) {
            return LegacyUsersDeprecationStage.READ_ONLY;
        }
        return LegacyUsersDeprecationStage.ONLY_WARNING;
    }

    public void assertLegacyReadAllowed(AuthenticatedUser actor, String operation) {
        if (properties.isReadEnabled()) {
            return;
        }
        telemetryService.recordLegacyUsersBlocked(actor, operation, actor != null ? actor.tenantId() : null, "read_disabled");
        throw new LegacyUsersOperationDisabledException(
                HttpStatus.NOT_FOUND,
                "Legacy /api/users read operations are disabled in this environment. Use membership-first endpoints.",
                operation,
                currentStage());
    }

    public void assertLegacyWriteAllowed(AuthenticatedUser actor, String operation) {
        if (properties.isWriteEnabled()) {
            return;
        }
        telemetryService.recordLegacyUsersBlocked(actor, operation, actor != null ? actor.tenantId() : null, "write_disabled");
        throw new LegacyUsersOperationDisabledException(
                HttpStatus.CONFLICT,
                "Legacy /api/users write operations are disabled by feature flag. Use membership-first endpoints.",
                operation,
                currentStage());
    }

    public void applyDeprecationHeaders(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Warning", "299 - \"/api/users is deprecated; use /api/access/users/{userId}/memberships\"");
        response.setHeader("X-Legacy-Users-Endpoint", "true");
        response.setHeader("X-Legacy-Users-Replacement", REPLACEMENT_PATH);
        response.setHeader("X-Legacy-Users-Stage", currentStage().name());
        response.setHeader("X-Legacy-Users-UI-Enabled", Boolean.toString(properties.isUiEnabled()));
        response.setHeader("X-Legacy-Users-Read-Enabled", Boolean.toString(properties.isReadEnabled()));
        response.setHeader("X-Legacy-Users-Write-Enabled", Boolean.toString(properties.isWriteEnabled()));
    }
}
