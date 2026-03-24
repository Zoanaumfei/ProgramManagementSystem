package com.oryzem.programmanagementsystem.platform.auth;

public interface CurrentUserEmailVerificationGateway {

    CurrentUserEmailVerificationState describeCurrentUser(AuthenticatedIdentityContext context);

    EmailVerificationCodeDelivery sendCurrentUserEmailVerificationCode(AuthenticatedIdentityContext context);

    CurrentUserEmailVerificationState verifyCurrentUserEmail(
            AuthenticatedIdentityContext context,
            String code);
}
