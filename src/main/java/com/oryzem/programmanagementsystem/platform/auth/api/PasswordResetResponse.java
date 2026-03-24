package com.oryzem.programmanagementsystem.platform.auth.api;

import com.oryzem.programmanagementsystem.platform.auth.PasswordResetDelivery;

public record PasswordResetResponse(
        String status,
        String username,
        String deliveryMedium,
        String destination) {

    static PasswordResetResponse codeSent(PasswordResetDelivery delivery) {
        return new PasswordResetResponse(
                "CODE_SENT",
                delivery.username(),
                delivery.deliveryMedium(),
                delivery.destination());
    }

    static PasswordResetResponse confirmed(String username) {
        return new PasswordResetResponse("PASSWORD_RESET_CONFIRMED", username, null, null);
    }
}
