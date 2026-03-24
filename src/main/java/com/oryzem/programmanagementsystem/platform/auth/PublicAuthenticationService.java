package com.oryzem.programmanagementsystem.platform.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PublicAuthenticationService {

    private static final String USERNAME_CLAIM = "cognito:username";
    private static final String ACCESS_TOKEN_USERNAME_CLAIM = "username";

    private final PublicAuthenticationGateway publicAuthenticationGateway;

    public PublicAuthenticationService(PublicAuthenticationGateway publicAuthenticationGateway) {
        this.publicAuthenticationGateway = publicAuthenticationGateway;
    }

    public PublicAuthenticationResult authenticate(String username, String password) {
        return publicAuthenticationGateway.authenticate(username, password);
    }

    public PublicAuthenticationResult completeNewPasswordChallenge(String username, String session, String newPassword) {
        return publicAuthenticationGateway.completeNewPasswordChallenge(username, session, newPassword);
    }

    public PasswordResetDelivery sendPasswordResetCode(String username) {
        return publicAuthenticationGateway.sendPasswordResetCode(username);
    }

    public void confirmPasswordReset(String username, String code, String newPassword) {
        publicAuthenticationGateway.confirmPasswordReset(username, code, newPassword);
    }

    public PublicAuthenticationTokens refreshSession(String username, String refreshToken) {
        return publicAuthenticationGateway.refreshSession(username, refreshToken);
    }

    public void signOut(Authentication authentication) {
        publicAuthenticationGateway.signOut(identityContext(authentication));
    }

    private AuthenticatedIdentityContext identityContext(Authentication authentication) {
        JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthentication.getToken();

        return new AuthenticatedIdentityContext(
                jwt.getSubject(),
                firstNonBlank(
                        jwt.getClaimAsString(USERNAME_CLAIM),
                        jwt.getClaimAsString(ACCESS_TOKEN_USERNAME_CLAIM),
                        jwtAuthentication.getName()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("token_use"),
                jwt.getTokenValue(),
                jwt.getClaimAsBoolean("email_verified"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "unknown";
    }
}
