package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;

final class CognitoUserIdentityGateway implements UserIdentityGateway, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CognitoUserIdentityGateway.class);

    private final UserIdentityProperties properties;
    private final CognitoIdentityProviderClient client;

    CognitoUserIdentityGateway(
            UserIdentityProperties properties,
            CognitoIdentityProviderClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public void createUser(ManagedUser user) {
        client.adminCreateUser(AdminCreateUserRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(user))
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .userAttributes(attributes(user))
                .build());
        syncRoleGroup(null, user);
    }

    @Override
    public void updateUser(ManagedUser existingUser, ManagedUser updatedUser) {
        client.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(updatedUser))
                .userAttributes(attributes(updatedUser))
                .build());
        syncRoleGroup(existingUser.role(), updatedUser);
    }

    @Override
    public void resendInvite(ManagedUser user) {
        client.adminCreateUser(AdminCreateUserRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(user))
                .messageAction(MessageActionType.RESEND)
                .userAttributes(attributes(user))
                .build());
    }

    @Override
    public void resetAccess(ManagedUser user) {
        client.adminResetUserPassword(AdminResetUserPasswordRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(user))
                .build());
    }

    @Override
    public void disableUser(ManagedUser user) {
        client.adminDisableUser(AdminDisableUserRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(user))
                .build());
    }

    @Override
    public void deleteUser(ManagedUser user) {
        if (!StringUtils.hasText(user.identityUsername())) {
            return;
        }

        try {
            client.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .build());
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException exception) {
            // Idempotent cleanup: absence in Cognito is acceptable during purge flows.
        }
    }

    @Override
    public boolean identityExists(ManagedUser user) {
        try {
            client.adminGetUser(AdminGetUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .build());
            return true;
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException exception) {
            return false;
        }
    }

    @Override
    public void ensureBootstrapUser(ManagedUser user, Set<Role> grantedRoles, String password, String temporaryPassword) {
        Set<Role> effectiveRoles = normalizeRoles(grantedRoles, user.role());
        if (identityExists(user)) {
            client.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .userAttributes(bootstrapAttributes(user))
                    .build());
        } else {
            AdminCreateUserRequest.Builder requestBuilder = AdminCreateUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .userAttributes(bootstrapAttributes(user));
            if (StringUtils.hasText(password)) {
                requestBuilder.messageAction(MessageActionType.SUPPRESS);
                requestBuilder.temporaryPassword(password);
            } else {
                requestBuilder.desiredDeliveryMediums(DeliveryMediumType.EMAIL);
                if (StringUtils.hasText(temporaryPassword)) {
                    requestBuilder.temporaryPassword(temporaryPassword);
                }
            }
            client.adminCreateUser(requestBuilder.build());
        }

        if (StringUtils.hasText(password)) {
            try {
                client.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
                        .userPoolId(properties.userPoolId())
                        .username(requiredIdentityUsername(user))
                        .password(password)
                        .permanent(true)
                        .build());
            } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException exception) {
                log.warn(
                        "Unable to set permanent password for bootstrap user '{}' in Cognito. "
                                + "The user was still reconciled and may need the first-login password change flow. reason={}",
                        requiredIdentityUsername(user),
                        exception.awsErrorDetails() != null ? exception.awsErrorDetails().errorMessage() : exception.getMessage());
            }
        }

        for (Role role : effectiveRoles) {
            client.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .groupName(role.name())
                    .build());
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private void syncRoleGroup(Role previousRole, ManagedUser user) {
        if (previousRole != null && previousRole != user.role()) {
            client.adminRemoveUserFromGroup(AdminRemoveUserFromGroupRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .groupName(previousRole.name())
                    .build());
        }

        if (previousRole != user.role()) {
            client.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .groupName(user.role().name())
                    .build());
        }
    }

    private List<AttributeType> attributes(ManagedUser user) {
        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attribute("email", user.email()));
        attributes.add(attribute("name", user.displayName()));
        attributes.add(attribute("custom:tenant_id", user.tenantId()));
        attributes.add(attribute("custom:tenant_type", user.tenantType().name()));
        attributes.add(attribute("custom:user_status", user.status().name()));
        return attributes;
    }

    private List<AttributeType> bootstrapAttributes(ManagedUser user) {
        List<AttributeType> attributes = attributes(user);
        attributes.add(attribute("email_verified", "true"));
        return attributes;
    }

    private AttributeType attribute(String name, String value) {
        return AttributeType.builder()
                .name(name)
                .value(value)
                .build();
    }

    private String requiredIdentityUsername(ManagedUser user) {
        if (!StringUtils.hasText(user.identityUsername())) {
            throw new IllegalStateException("User identity username is required for Cognito operations.");
        }
        return user.identityUsername();
    }

    private Set<Role> normalizeRoles(Set<Role> grantedRoles, Role primaryRole) {
        LinkedHashSet<Role> normalizedRoles = new LinkedHashSet<>();
        if (primaryRole != null) {
            normalizedRoles.add(primaryRole);
        }
        if (grantedRoles != null) {
            normalizedRoles.addAll(grantedRoles);
        }
        if (normalizedRoles.isEmpty()) {
            normalizedRoles.add(Role.ADMIN);
        }
        return normalizedRoles;
    }
}
