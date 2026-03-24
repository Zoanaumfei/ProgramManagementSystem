package com.oryzem.programmanagementsystem.platform.auth.api;

import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationState;

public record EmailVerificationResponse(
        String email,
        boolean emailVerified,
        boolean emailVerificationRequired,
        String status,
        String attributeName,
        String deliveryMedium,
        String destination) {

    public static EmailVerificationResponse snapshot(CurrentUserEmailVerificationState state) {
        return new EmailVerificationResponse(
                state.email(),
                state.emailVerified(),
                state.emailVerificationRequired(),
                state.emailVerified() ? "VERIFIED" : "PENDING_VERIFICATION",
                "email",
                null,
                null);
    }

    public static EmailVerificationResponse codeSent(
            CurrentUserEmailVerificationState state,
            String attributeName,
            String deliveryMedium,
            String destination) {
        return new EmailVerificationResponse(
                state.email(),
                state.emailVerified(),
                state.emailVerificationRequired(),
                "CODE_SENT",
                attributeName,
                deliveryMedium,
                destination);
    }

    public static EmailVerificationResponse verified(CurrentUserEmailVerificationState state) {
        return snapshot(new CurrentUserEmailVerificationState(state.email(), true));
    }
}
