package com.oryzem.programmanagementsystem.platform.auth;

public record PublicAuthenticationResult(
        String status,
        String username,
        String challengeName,
        String session,
        PublicAuthenticationTokens tokens) {

    public static PublicAuthenticationResult authenticated(String username, PublicAuthenticationTokens tokens) {
        return new PublicAuthenticationResult("AUTHENTICATED", username, null, null, tokens);
    }

    public static PublicAuthenticationResult newPasswordRequired(String username, String session) {
        return new PublicAuthenticationResult("NEW_PASSWORD_REQUIRED", username, "NEW_PASSWORD_REQUIRED", session, null);
    }

    public static PublicAuthenticationResult passwordResetRequired(String username) {
        return new PublicAuthenticationResult("PASSWORD_RESET_REQUIRED", username, "PASSWORD_RESET_REQUIRED", null, null);
    }
}
