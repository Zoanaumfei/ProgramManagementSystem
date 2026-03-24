package com.oryzem.programmanagementsystem.platform.auth;

public record PasswordResetDelivery(
        String username,
        String deliveryMedium,
        String destination) {
}
