package com.oryzem.programmanagementsystem.web;

import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.users.CreateUserRequest;
import com.oryzem.programmanagementsystem.users.UserActionResponse;
import com.oryzem.programmanagementsystem.users.UserManagementService;
import com.oryzem.programmanagementsystem.users.UserSummaryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public UserManagementController(
            UserManagementService userManagementService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.userManagementService = userManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping
    public List<UserSummaryResponse> listUsers(
            Authentication authentication,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return userManagementService.listUsers(actor, tenantId, supportOverride, justification);
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        UserSummaryResponse created = userManagementService.createUser(actor, request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            Authentication authentication,
            @PathVariable String userId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        userManagementService.deleteUser(actor, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/resend-invite")
    public UserActionResponse resendInvite(
            Authentication authentication,
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return userManagementService.resendInvite(actor, userId, supportOverride, justification);
    }

    @PostMapping("/{userId}/reset-access")
    public UserActionResponse resetAccess(
            Authentication authentication,
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return userManagementService.resetAccess(actor, userId, supportOverride, justification);
    }
}
