package com.oryzem.programmanagementsystem.platform.auth;

public record EmailVerificationCodeDelivery(
        String attributeName,
        String deliveryMedium,
        String destination) {
}
