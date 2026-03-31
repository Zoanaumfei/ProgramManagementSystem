package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.auth.AuthenticatedIdentityContext;
import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationGateway;
import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationState;
import com.oryzem.programmanagementsystem.platform.auth.EmailVerificationCodeDelivery;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.shared.RateLimitExceededException;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeDeliveryDetailsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.LimitExceededException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifyUserAttributeRequest;

final class CognitoUserIdentityGateway implements UserIdentityGateway, CurrentUserEmailVerificationGateway, AutoCloseable {

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
        try {
            client.adminCreateUser(AdminCreateUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .userAttributes(attributes(user))
                    .build());
        } catch (UsernameExistsException exception) {
            throw new ConflictException(
                    "A user account with this email already exists in Cognito. Use the existing account or clean it up before retrying.",
                    exception);
        }
    }

    @Override
    public void updateUser(ManagedUser existingUser, ManagedUser updatedUser) {
        client.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(properties.userPoolId())
                .username(requiredIdentityUsername(updatedUser))
                .userAttributes(attributes(updatedUser))
                .build());
    }

    @Override
    public void resendInvite(ManagedUser user) {
        try {
            client.adminCreateUser(AdminCreateUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .messageAction(MessageActionType.RESEND)
                    .userAttributes(attributes(user))
                    .build());
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked invite resend attempts for this user. Wait a few minutes and try again.",
                    exception);
        }
    }

    @Override
    public void resetAccess(ManagedUser user) {
        try {
            client.adminResetUserPassword(AdminResetUserPasswordRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .build());
        } catch (InvalidParameterException exception) {
            if (requiresVerifiedRecoveryChannel(exception)) {
                throw new ConflictException("The user must verify email before access reset can be used.", exception);
            }
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked password reset attempts for this user. Wait a few minutes and try again.",
                    exception);
        }
    }

    @Override
    public void disableUser(ManagedUser user) {
        try {
            client.adminDisableUser(AdminDisableUserRequest.builder()
                    .userPoolId(properties.userPoolId())
                    .username(requiredIdentityUsername(user))
                    .build());
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException exception) {
            // Idempotent cleanup: absence in Cognito is acceptable during inactivation flows.
        }
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
        Set<Role> effectiveRoles = normalizeRoles(grantedRoles);
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

    @Override
    public CurrentUserEmailVerificationState describeCurrentUser(AuthenticatedIdentityContext context) {
        if (!isAccessTokenContext(context)) {
            return new CurrentUserEmailVerificationState(
                    context.email(),
                    Boolean.TRUE.equals(context.emailVerifiedClaim()));
        }

        GetUserResponse response = client.getUser(GetUserRequest.builder()
                .accessToken(context.bearerToken())
                .build());
        String email = attributeValue(response.userAttributes(), "email");
        boolean emailVerified = Boolean.parseBoolean(attributeValue(response.userAttributes(), "email_verified"));
        return new CurrentUserEmailVerificationState(
                StringUtils.hasText(email) ? email : context.email(),
                emailVerified);
    }

    @Override
    public EmailVerificationCodeDelivery sendCurrentUserEmailVerificationCode(AuthenticatedIdentityContext context) {
        requireAccessTokenContext(context);

        try {
            CodeDeliveryDetailsType delivery = client.getUserAttributeVerificationCode(GetUserAttributeVerificationCodeRequest.builder()
                            .accessToken(context.bearerToken())
                            .attributeName("email")
                            .build())
                    .codeDeliveryDetails();
            return new EmailVerificationCodeDelivery(
                    "email",
                    delivery != null && delivery.deliveryMedium() != null ? delivery.deliveryMedium().name() : null,
                    delivery != null ? delivery.destination() : null);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked email verification attempts. Wait a few minutes and try again.",
                    exception);
        } catch (NotAuthorizedException exception) {
            throw new IllegalArgumentException("Email verification requires a valid Cognito access token. Sign in again and try once more.");
        }
    }

    @Override
    public CurrentUserEmailVerificationState verifyCurrentUserEmail(AuthenticatedIdentityContext context, String code) {
        requireAccessTokenContext(context);

        try {
            client.verifyUserAttribute(VerifyUserAttributeRequest.builder()
                    .accessToken(context.bearerToken())
                    .attributeName("email")
                    .code(code)
                    .build());
            return new CurrentUserEmailVerificationState(context.email(), true);
        } catch (CodeMismatchException exception) {
            throw new IllegalArgumentException("The email verification code is invalid.");
        } catch (ExpiredCodeException exception) {
            throw new IllegalArgumentException("The email verification code has expired. Request a new code and try again.");
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked email verification attempts. Wait a few minutes and try again.",
                    exception);
        } catch (NotAuthorizedException exception) {
            throw new IllegalArgumentException("Email verification requires a valid Cognito access token. Sign in again and try once more.");
        }
    }

    private List<AttributeType> attributes(ManagedUser user) {
        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attribute("email", user.email()));
        attributes.add(attribute("name", user.displayName()));
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

    private Set<Role> normalizeRoles(Set<Role> grantedRoles) {
        LinkedHashSet<Role> normalizedRoles = new LinkedHashSet<>();
        if (grantedRoles != null) {
            normalizedRoles.addAll(grantedRoles);
        }
        if (normalizedRoles.isEmpty()) {
            normalizedRoles.add(Role.ADMIN);
        }
        return normalizedRoles;
    }

    private boolean isAccessTokenContext(AuthenticatedIdentityContext context) {
        return "access".equalsIgnoreCase(context.tokenUse()) && StringUtils.hasText(context.bearerToken());
    }

    private void requireAccessTokenContext(AuthenticatedIdentityContext context) {
        if (!isAccessTokenContext(context)) {
            throw new IllegalArgumentException("Email verification requires a valid Cognito access token. Sign in again and try once more.");
        }
    }

    private String attributeValue(List<AttributeType> attributes, String attributeName) {
        return attributes.stream()
                .filter(attribute -> attributeName.equals(attribute.name()))
                .map(AttributeType::value)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private boolean requiresVerifiedRecoveryChannel(InvalidParameterException exception) {
        return resolveAwsMessage(exception).toLowerCase().contains("no registered/verified email or phone_number");
    }

    private String resolveAwsMessage(CognitoIdentityProviderException exception) {
        if (exception.awsErrorDetails() != null && StringUtils.hasText(exception.awsErrorDetails().errorMessage())) {
            return exception.awsErrorDetails().errorMessage();
        }
        return exception.getMessage();
    }
}
