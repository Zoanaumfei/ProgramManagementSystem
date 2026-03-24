package com.oryzem.programmanagementsystem.platform.auth;

public interface PublicAuthenticationGateway {

    PublicAuthenticationResult authenticate(String username, String password);

    PublicAuthenticationResult completeNewPasswordChallenge(String username, String session, String newPassword);

    PasswordResetDelivery sendPasswordResetCode(String username);

    void confirmPasswordReset(String username, String code, String newPassword);

    PublicAuthenticationTokens refreshSession(String username, String refreshToken);

    void signOut(AuthenticatedIdentityContext context);
}
