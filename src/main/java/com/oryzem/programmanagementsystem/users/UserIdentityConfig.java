package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;

@Configuration
@EnableConfigurationProperties(UserIdentityProperties.class)
class UserIdentityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "cognito")
    UserIdentityGateway cognitoUserIdentityGateway(UserIdentityProperties properties) {
        if (!StringUtils.hasText(properties.userPoolId())) {
            throw new IllegalStateException("Cognito identity integration requires app.security.identity.user-pool-id.");
        }

        CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        return new CognitoUserIdentityGateway(properties, client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.identity", name = "provider", havingValue = "stub", matchIfMissing = true)
    StubUserIdentityGateway stubUserIdentityGateway() {
        return new StubUserIdentityGateway();
    }
}

@ConfigurationProperties(prefix = "app.security.identity")
record UserIdentityProperties(
        String provider,
        String userPoolId,
        String region) {
}

interface UserIdentityGateway {

    void createUser(ManagedUser user);

    void updateUser(ManagedUser existingUser, ManagedUser updatedUser);

    void resendInvite(ManagedUser user);

    void resetAccess(ManagedUser user);

    void disableUser(ManagedUser user);

    boolean identityExists(ManagedUser user);
}

record StubUserIdentityOperation(
        String action,
        String identityUsername,
        String email,
        Role role,
        String tenantId,
        TenantType tenantType) {
}

final class StubUserIdentityGateway implements UserIdentityGateway {

    private final List<StubUserIdentityOperation> operations = new ArrayList<>();
    private final java.util.Set<String> existingIdentityUsernames = new java.util.HashSet<>();

    @Override
    public void createUser(ManagedUser user) {
        operations.add(operation("CREATE", user));
        trackIdentity(user);
    }

    @Override
    public void updateUser(ManagedUser existingUser, ManagedUser updatedUser) {
        operations.add(operation("UPDATE", updatedUser));
    }

    @Override
    public void resendInvite(ManagedUser user) {
        operations.add(operation("RESEND_INVITE", user));
        trackIdentity(user);
    }

    @Override
    public void resetAccess(ManagedUser user) {
        operations.add(operation("RESET_ACCESS", user));
    }

    @Override
    public void disableUser(ManagedUser user) {
        operations.add(operation("DISABLE", user));
    }

    @Override
    public boolean identityExists(ManagedUser user) {
        return user.identityUsername() != null
                && existingIdentityUsernames.contains(user.identityUsername().toLowerCase(java.util.Locale.ROOT));
    }

    List<StubUserIdentityOperation> operations() {
        return List.copyOf(operations);
    }

    void markIdentityPresent(String identityUsername) {
        if (identityUsername != null && !identityUsername.isBlank()) {
            existingIdentityUsernames.add(identityUsername.toLowerCase(java.util.Locale.ROOT));
        }
    }

    void markIdentityMissing(String identityUsername) {
        if (identityUsername != null && !identityUsername.isBlank()) {
            existingIdentityUsernames.remove(identityUsername.toLowerCase(java.util.Locale.ROOT));
        }
    }

    void clear() {
        operations.clear();
        existingIdentityUsernames.clear();
    }

    private StubUserIdentityOperation operation(String action, ManagedUser user) {
        return new StubUserIdentityOperation(
                action,
                user.identityUsername(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.tenantType());
    }

    private void trackIdentity(ManagedUser user) {
        markIdentityPresent(user.identityUsername());
    }
}

final class CognitoUserIdentityGateway implements UserIdentityGateway, AutoCloseable {

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
}
