package com.oryzem.programmanagementsystem.platform.auth.api;

import com.oryzem.programmanagementsystem.platform.auth.PublicAuthenticationResult;
import com.oryzem.programmanagementsystem.platform.auth.PublicAuthenticationTokens;

public record AuthenticationResponse(
        String status,
        String username,
        String challengeName,
        String session,
        String accessToken,
        String idToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType) {

    static AuthenticationResponse from(PublicAuthenticationResult result) {
        PublicAuthenticationTokens tokens = result.tokens();
        return new AuthenticationResponse(
                result.status(),
                result.username(),
                result.challengeName(),
                result.session(),
                tokens != null ? tokens.accessToken() : null,
                tokens != null ? tokens.idToken() : null,
                tokens != null ? tokens.refreshToken() : null,
                tokens != null ? tokens.expiresIn() : null,
                tokens != null ? tokens.tokenType() : null);
    }

    static AuthenticationResponse authenticated(String username, PublicAuthenticationTokens tokens) {
        return from(PublicAuthenticationResult.authenticated(username, tokens));
    }
}
