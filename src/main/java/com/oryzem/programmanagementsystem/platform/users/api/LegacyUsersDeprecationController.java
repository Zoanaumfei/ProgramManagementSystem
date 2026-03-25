package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.audit.AccessAdoptionReportResponse;
import com.oryzem.programmanagementsystem.platform.audit.AccessAdoptionReportService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersFeatureFlagService;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/legacy-users")
public class LegacyUsersDeprecationController {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final LegacyUsersFeatureFlagService featureFlagService;
    private final AccessAdoptionReportService adoptionReportService;

    public LegacyUsersDeprecationController(
            AuthenticatedUserMapper authenticatedUserMapper,
            LegacyUsersFeatureFlagService featureFlagService,
            AccessAdoptionReportService adoptionReportService) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.featureFlagService = featureFlagService;
        this.adoptionReportService = adoptionReportService;
    }

    @GetMapping("/deprecation-status")
    public LegacyUsersDeprecationStatusResponse deprecationStatus(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        assertOperatorAccess(actor);
        return new LegacyUsersDeprecationStatusResponse(
                featureFlagService.isUiEnabled(),
                featureFlagService.isReadEnabled(),
                featureFlagService.isWriteEnabled(),
                featureFlagService.currentStage(),
                LegacyUsersFeatureFlagService.LEGACY_USERS_PATH,
                LegacyUsersFeatureFlagService.REPLACEMENT_PATH,
                Instant.now());
    }

    @GetMapping("/adoption-report")
    public AccessAdoptionReportResponse adoptionReport(
            Authentication authentication,
            @RequestParam(defaultValue = "56") int trailingDays) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        assertOperatorAccess(actor);
        return adoptionReportService.generateReport(trailingDays);
    }

    private void assertOperatorAccess(AuthenticatedUser actor) {
        if (actor.tenantType() == TenantType.INTERNAL && (actor.hasRole(Role.ADMIN) || actor.hasRole(Role.SUPPORT))) {
            return;
        }
        throw new AccessDeniedException("Legacy users deprecation telemetry is restricted to internal operators.");
    }
}
