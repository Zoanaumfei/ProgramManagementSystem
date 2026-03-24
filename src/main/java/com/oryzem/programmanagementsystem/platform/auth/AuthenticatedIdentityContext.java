package com.oryzem.programmanagementsystem.platform.auth;

public record AuthenticatedIdentityContext(
        String subject,
        String username,
        String email,
        String tokenUse,
        String bearerToken,
        Boolean emailVerifiedClaim) {
}
