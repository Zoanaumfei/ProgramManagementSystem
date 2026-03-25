package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.platform.users.application.UserManagementService;
import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersFeatureFlagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final LegacyUsersFeatureFlagService featureFlagService;

    public UserManagementController(
            UserManagementService userManagementService,
            AuthenticatedUserMapper authenticatedUserMapper,
            LegacyUsersFeatureFlagService featureFlagService) {
        this.userManagementService = userManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.featureFlagService = featureFlagService;
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @GetMapping
    public List<UserSummaryResponse> listUsers(
            Authentication authentication,
            @RequestParam(required = false) String organizationId,
            @RequestParam(name = "tenantId", required = false) String legacyTenantId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyReadAllowed(actor, "list");
        return userManagementService.listUsers(
                actor,
                firstNonBlank(organizationId, legacyTenantId),
                supportOverride,
                justification);
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @PostMapping
    public ResponseEntity<UserSummaryResponse> createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "create");
        UserSummaryResponse created = userManagementService.createUser(actor, request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @PutMapping("/{userId}")
    public UserSummaryResponse updateUser(
            Authentication authentication,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "update");
        return userManagementService.updateUser(actor, userId, request);
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            Authentication authentication,
            @PathVariable String userId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "delete");
        userManagementService.deleteUser(actor, userId);
        return ResponseEntity.noContent().build();
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @PostMapping("/{userId}/resend-invite")
    public UserActionResponse resendInvite(
            Authentication authentication,
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "resend_invite");
        return userManagementService.resendInvite(actor, userId, supportOverride, justification);
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @PostMapping("/{userId}/reset-access")
    public UserActionResponse resetAccess(
            Authentication authentication,
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "reset_access");
        return userManagementService.resetAccess(actor, userId, supportOverride, justification);
    }

    @Deprecated(forRemoval = false, since = "2026-03-25")
    @PostMapping("/{userId}/purge")
    public UserActionResponse purgeUser(
            Authentication authentication,
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        featureFlagService.assertLegacyWriteAllowed(actor, "purge");
        return userManagementService.purgeUser(actor, userId, supportOverride, justification);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}

