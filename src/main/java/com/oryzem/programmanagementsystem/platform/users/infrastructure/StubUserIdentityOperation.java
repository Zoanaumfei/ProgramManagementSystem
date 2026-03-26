package com.oryzem.programmanagementsystem.platform.users.infrastructure;

public record StubUserIdentityOperation(
        String action,
        String identityUsername,
        String email) {
}
