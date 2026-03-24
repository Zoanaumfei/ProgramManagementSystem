package com.oryzem.programmanagementsystem.platform.auth;

public record PublicAuthenticationTokens(
        String accessToken,
        String idToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType) {
}
