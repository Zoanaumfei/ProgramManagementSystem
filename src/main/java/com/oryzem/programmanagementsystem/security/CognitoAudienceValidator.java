package com.oryzem.programmanagementsystem.security;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class CognitoAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitoAudienceValidator.class);

    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token",
            "The token does not contain the expected Cognito app client identifier.",
            null);

    private final String appClientId;

    public CognitoAudienceValidator(String appClientId) {
        this.appClientId = appClientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (matchesAudience(token) || matchesClientId(token)) {
            return OAuth2TokenValidatorResult.success();
        }

        LOGGER.warn(
                "Rejected JWT due to audience/client mismatch. subject={}, tokenUse={}",
                token.getSubject(),
                token.getClaimAsString("token_use"));
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }

    private boolean matchesAudience(Jwt token) {
        List<String> audience = token.getAudience();
        return audience != null && audience.contains(appClientId);
    }

    private boolean matchesClientId(Jwt token) {
        String clientId = token.getClaimAsString("client_id");
        return appClientId.equals(clientId);
    }
}
