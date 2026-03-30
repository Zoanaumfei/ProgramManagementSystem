package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.users.api.CreateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UpdateUserRequest;
import com.oryzem.programmanagementsystem.platform.users.api.UserActionResponse;
import com.oryzem.programmanagementsystem.platform.users.api.UserSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserManagementService {

    private final UserQueryService queryService;
    private final UserCommandService commandService;
    private final UserSensitiveActionService sensitiveActionService;
    private final UserPurgeService purgeService;
    private final AuthenticatedUserSynchronizationService synchronizationService;

    public UserManagementService(
            UserQueryService queryService,
            UserCommandService commandService,
            UserSensitiveActionService sensitiveActionService,
            UserPurgeService purgeService,
            AuthenticatedUserSynchronizationService synchronizationService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.sensitiveActionService = sensitiveActionService;
        this.purgeService = purgeService;
        this.synchronizationService = synchronizationService;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers(
            AuthenticatedUser actor,
            String organizationId,
            boolean supportOverride,
            String justification) {
        return queryService.listUsers(actor, organizationId, supportOverride, justification);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listOrphanUsers(AuthenticatedUser actor) {
        return queryService.listOrphanUsers(actor);
    }

    public UserSummaryResponse createUser(AuthenticatedUser actor, CreateUserRequest request) {
        return commandService.createUser(actor, request);
    }

    public UserSummaryResponse updateUser(AuthenticatedUser actor, String userId, UpdateUserRequest request) {
        return commandService.updateUser(actor, userId, request);
    }

    public void deleteUser(AuthenticatedUser actor, String userId) {
        commandService.deleteUser(actor, userId);
    }

    public UserActionResponse resendInvite(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return sensitiveActionService.resendInvite(actor, userId, supportOverride, justification);
    }

    public UserActionResponse resetAccess(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return sensitiveActionService.resetAccess(actor, userId, supportOverride, justification);
    }

    public UserActionResponse purgeUser(
            AuthenticatedUser actor,
            String userId,
            boolean supportOverride,
            String justification) {
        return purgeService.purgeUser(actor, userId, supportOverride, justification);
    }

    public void synchronizeAuthenticatedUser(String identitySubject, String identityUsername, String email) {
        synchronizationService.synchronizeAuthenticatedUser(identitySubject, identityUsername, email);
    }
}
