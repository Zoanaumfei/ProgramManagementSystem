package com.oryzem.programmanagementsystem.platform.auth;

import com.oryzem.programmanagementsystem.platform.shared.RateLimitExceededException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class StubPublicAuthenticationGateway implements PublicAuthenticationGateway {

    private static final String DEFAULT_PASSWORD = "Password123!";
    private static final String DEFAULT_TEMP_PASSWORD = "Temp123!";
    private static final String DEFAULT_RESET_CODE = "654321";

    private final Map<String, StubUserSession> users = new ConcurrentHashMap<>();

    @Override
    public PublicAuthenticationResult authenticate(String username, String password) {
        StubUserSession user = users.computeIfAbsent(username, StubUserSession::new);
        if ("rate-limit@oryzem.com".equalsIgnoreCase(username)) {
            throw new RateLimitExceededException("Stub rate limit hit for authentication.");
        }
        if (user.passwordResetRequired()) {
            return PublicAuthenticationResult.passwordResetRequired(username);
        }
        if (user.newPasswordRequired()) {
            if (!DEFAULT_TEMP_PASSWORD.equals(password)) {
                throw new AuthenticationFailedException("Invalid credentials.");
            }
            user.session(randomValue("session"));
            return PublicAuthenticationResult.newPasswordRequired(username, user.session());
        }
        if (!user.password().equals(password)) {
            throw new AuthenticationFailedException("Invalid credentials.");
        }
        return PublicAuthenticationResult.authenticated(username, tokens(username, user.refreshToken()));
    }

    @Override
    public PublicAuthenticationResult completeNewPasswordChallenge(String username, String session, String newPassword) {
        StubUserSession user = users.computeIfAbsent(username, StubUserSession::new);
        if (!user.newPasswordRequired() || !session.equals(user.session())) {
            throw new AuthenticationFailedException("The new password challenge session is no longer valid.");
        }
        user.password(newPassword);
        user.newPasswordRequired(false);
        user.session(null);
        return PublicAuthenticationResult.authenticated(username, tokens(username, user.refreshToken()));
    }

    @Override
    public PasswordResetDelivery sendPasswordResetCode(String username) {
        StubUserSession user = users.computeIfAbsent(username, StubUserSession::new);
        user.passwordResetRequired(true);
        user.resetCode(DEFAULT_RESET_CODE);
        return new PasswordResetDelivery(username, "EMAIL", maskedDestination(username));
    }

    @Override
    public void confirmPasswordReset(String username, String code, String newPassword) {
        StubUserSession user = users.computeIfAbsent(username, StubUserSession::new);
        if (!DEFAULT_RESET_CODE.equals(code) || !DEFAULT_RESET_CODE.equals(user.resetCode())) {
            throw new IllegalArgumentException("Invalid password reset code.");
        }
        user.password(newPassword);
        user.passwordResetRequired(false);
        user.resetCode(null);
    }

    @Override
    public PublicAuthenticationTokens refreshSession(String username, String refreshToken) {
        StubUserSession user = users.computeIfAbsent(username, StubUserSession::new);
        if (!user.refreshToken().equals(refreshToken)) {
            throw new AuthenticationFailedException("The refresh token is no longer valid.");
        }
        return tokens(username, refreshToken);
    }

    @Override
    public void signOut(AuthenticatedIdentityContext context) {
        if (context == null) {
            return;
        }
    }

    private PublicAuthenticationTokens tokens(String username, String refreshToken) {
        return new PublicAuthenticationTokens(
                randomValue("access-" + username),
                randomValue("id-" + username),
                refreshToken,
                3600,
                "Bearer");
    }

    private String maskedDestination(String username) {
        int atIndex = username.indexOf('@');
        if (atIndex <= 1) {
            return username;
        }
        return username.charAt(0) + "***" + username.substring(atIndex);
    }

    private String randomValue(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static final class StubUserSession {

        private String password = DEFAULT_PASSWORD;
        private final String refreshToken = "refresh-" + UUID.randomUUID();
        private boolean newPasswordRequired;
        private boolean passwordResetRequired;
        private String session;
        private String resetCode;

        private StubUserSession(String username) {
            this.newPasswordRequired = username.contains("new-password");
            this.passwordResetRequired = username.contains("reset-required");
        }

        private String password() {
            return password;
        }

        private void password(String password) {
            this.password = password;
        }

        private String refreshToken() {
            return refreshToken;
        }

        private boolean newPasswordRequired() {
            return newPasswordRequired;
        }

        private void newPasswordRequired(boolean newPasswordRequired) {
            this.newPasswordRequired = newPasswordRequired;
        }

        private boolean passwordResetRequired() {
            return passwordResetRequired;
        }

        private void passwordResetRequired(boolean passwordResetRequired) {
            this.passwordResetRequired = passwordResetRequired;
        }

        private String session() {
            return session;
        }

        private void session(String session) {
            this.session = session;
        }

        private String resetCode() {
            return resetCode;
        }

        private void resetCode(String resetCode) {
            this.resetCode = resetCode;
        }
    }
}
