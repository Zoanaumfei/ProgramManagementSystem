package com.oryzem.programmanagementsystem.platform.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthEmailVerificationService {

    private static final String USERNAME_CLAIM = "cognito:username";
    private static final String ACCESS_TOKEN_USERNAME_CLAIM = "username";

    private final CurrentUserEmailVerificationGateway currentUserEmailVerificationGateway;

    public AuthEmailVerificationService(CurrentUserEmailVerificationGateway currentUserEmailVerificationGateway) {
        this.currentUserEmailVerificationGateway = currentUserEmailVerificationGateway;
    }

    public CurrentUserEmailVerificationState describe(Authentication authentication) {
        return currentUserEmailVerificationGateway.describeCurrentUser(identityContext(authentication));
    }

    public EmailVerificationCodeDelivery sendCode(Authentication authentication) {
        return currentUserEmailVerificationGateway.sendCurrentUserEmailVerificationCode(identityContext(authentication));
    }

    public CurrentUserEmailVerificationState confirmCode(Authentication authentication, String code) {
        return currentUserEmailVerificationGateway.verifyCurrentUserEmail(identityContext(authentication), code);
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
