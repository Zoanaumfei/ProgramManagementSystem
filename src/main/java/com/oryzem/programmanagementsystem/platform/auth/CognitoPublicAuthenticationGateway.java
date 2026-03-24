package com.oryzem.programmanagementsystem.platform.auth;

import com.oryzem.programmanagementsystem.platform.shared.RateLimitExceededException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeDeliveryDetailsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.LimitExceededException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;

final class CognitoPublicAuthenticationGateway implements PublicAuthenticationGateway {

    private static final Logger log = LoggerFactory.getLogger(CognitoPublicAuthenticationGateway.class);

    private final CognitoProperties cognitoProperties;
    private final CognitoIdentityProviderClient client;

    CognitoPublicAuthenticationGateway(
            CognitoProperties cognitoProperties,
            CognitoIdentityProviderClient client) {
        this.cognitoProperties = cognitoProperties;
        this.client = client;
    }

    @Override
    public PublicAuthenticationResult authenticate(String username, String password) {
        try {
            InitiateAuthResponse response = client.initiateAuth(InitiateAuthRequest.builder()
                    .clientId(cognitoProperties.appClientId())
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParameters(username, Map.of(
                            "USERNAME", username,
                            "PASSWORD", password)))
                    .build());
            return toAuthenticationResult(username, response.challengeName(), response.session(), response.authenticationResult());
        } catch (PasswordResetRequiredException exception) {
            return PublicAuthenticationResult.passwordResetRequired(username);
        } catch (NotAuthorizedException exception) {
            log.warn("Cognito login rejected for [{}]: {}", username, resolveAwsMessage(exception));
            throw new AuthenticationFailedException("Invalid credentials.", exception);
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked authentication attempts. Wait a few minutes and try again.",
                    exception);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        }
    }

    @Override
    public PublicAuthenticationResult completeNewPasswordChallenge(String username, String session, String newPassword) {
        try {
            RespondToAuthChallengeResponse response = client.respondToAuthChallenge(RespondToAuthChallengeRequest.builder()
                    .clientId(cognitoProperties.appClientId())
                    .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                    .session(session)
                    .challengeResponses(authParameters(username, Map.of(
                            "USERNAME", username,
                            "NEW_PASSWORD", newPassword)))
                    .build());
            return toAuthenticationResult(username, response.challengeName(), response.session(), response.authenticationResult());
        } catch (NotAuthorizedException exception) {
            throw new AuthenticationFailedException("The new password challenge session is no longer valid.", exception);
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked password change attempts. Wait a few minutes and try again.",
                    exception);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        }
    }

    @Override
    public PasswordResetDelivery sendPasswordResetCode(String username) {
        try {
            ForgotPasswordResponse response = client.forgotPassword(ForgotPasswordRequest.builder()
                    .clientId(cognitoProperties.appClientId())
                    .username(username)
                    .secretHash(secretHash(username))
                    .build());
            CodeDeliveryDetailsType delivery = response.codeDeliveryDetails();
            return new PasswordResetDelivery(
                    username,
                    delivery != null ? delivery.deliveryMediumAsString() : null,
                    delivery != null ? delivery.destination() : null);
        } catch (NotAuthorizedException exception) {
            throw new AuthenticationFailedException("Password reset is not available for this user.", exception);
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked password reset requests. Wait a few minutes and try again.",
                    exception);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        }
    }

    @Override
    public void confirmPasswordReset(String username, String code, String newPassword) {
        try {
            client.confirmForgotPassword(ConfirmForgotPasswordRequest.builder()
                    .clientId(cognitoProperties.appClientId())
                    .username(username)
                    .confirmationCode(code)
                    .password(newPassword)
                    .secretHash(secretHash(username))
                    .build());
        } catch (CodeMismatchException exception) {
            throw new IllegalArgumentException("Invalid password reset code.");
        } catch (ExpiredCodeException exception) {
            throw new IllegalArgumentException("Password reset code expired. Request a new code.");
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked password reset confirmation attempts. Wait a few minutes and try again.",
                    exception);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        }
    }

    @Override
    public PublicAuthenticationTokens refreshSession(String username, String refreshToken) {
        try {
            AuthenticationResultType result = client.initiateAuth(InitiateAuthRequest.builder()
                            .clientId(cognitoProperties.appClientId())
                            .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                            .authParameters(authParameters(username, Map.of("REFRESH_TOKEN", refreshToken)))
                            .build())
                    .authenticationResult();
            return tokens(result, refreshToken);
        } catch (NotAuthorizedException exception) {
            throw new AuthenticationFailedException("The refresh token is no longer valid.", exception);
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked session refresh attempts. Wait a few minutes and try again.",
                    exception);
        } catch (InvalidParameterException exception) {
            throw new IllegalArgumentException(resolveAwsMessage(exception));
        }
    }

    @Override
    public void signOut(AuthenticatedIdentityContext context) {
        try {
            client.globalSignOut(GlobalSignOutRequest.builder()
                    .accessToken(context.bearerToken())
                    .build());
        } catch (NotAuthorizedException exception) {
            throw new AuthenticationFailedException("The current session is no longer valid.", exception);
        } catch (LimitExceededException exception) {
            throw new RateLimitExceededException(
                    "Cognito temporarily blocked sign-out attempts. Wait a few minutes and try again.",
                    exception);
        }
    }

    private PublicAuthenticationResult toAuthenticationResult(
            String username,
            ChallengeNameType challengeName,
            String session,
            AuthenticationResultType authenticationResult) {
        if (authenticationResult != null) {
            return PublicAuthenticationResult.authenticated(username, tokens(authenticationResult, null));
        }
        if (challengeName == ChallengeNameType.NEW_PASSWORD_REQUIRED) {
            return PublicAuthenticationResult.newPasswordRequired(username, session);
        }
        throw new IllegalArgumentException("Unsupported Cognito challenge: " + challengeName);
    }

    private PublicAuthenticationTokens tokens(AuthenticationResultType result, String fallbackRefreshToken) {
        if (result == null) {
            throw new IllegalArgumentException("Cognito did not return authentication tokens.");
        }
        return new PublicAuthenticationTokens(
                result.accessToken(),
                result.idToken(),
                StringUtils.hasText(result.refreshToken()) ? result.refreshToken() : fallbackRefreshToken,
                result.expiresIn(),
                result.tokenType());
    }

    private Map<String, String> authParameters(String username, Map<String, String> parameters) {
        Map<String, String> result = new LinkedHashMap<>(parameters);
        String secretHash = secretHash(username);
        if (StringUtils.hasText(secretHash)) {
            result.put("SECRET_HASH", secretHash);
        }
        return result;
    }

    private String secretHash(String username) {
        if (!StringUtils.hasText(cognitoProperties.appClientSecret())) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    cognitoProperties.appClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] raw = mac.doFinal((username + cognitoProperties.appClientId()).getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(raw);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute Cognito secret hash.", exception);
        }
    }

    private String resolveAwsMessage(CognitoIdentityProviderException exception) {
        String message = exception.awsErrorDetails() != null
                ? exception.awsErrorDetails().errorMessage()
                : null;
        return StringUtils.hasText(message) ? message : "Cognito request failed.";
    }
}
