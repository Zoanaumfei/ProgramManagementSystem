package com.oryzem.programmanagementsystem.platform.users.domain;

import java.time.Instant;

public record ManagedUser(
        String id,
        String identityUsername,
        String identitySubject,
        String displayName,
        String email,
        UserStatus status,
        Instant createdAt,
        Instant inviteResentAt,
        Instant accessResetAt) {

    public ManagedUser withUpdatedDetails(String displayName, String email) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                status,
                createdAt,
                inviteResentAt,
                accessResetAt);
    }

    public ManagedUser withInviteResentAt(Instant performedAt) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                status,
                createdAt,
                performedAt,
                accessResetAt);
    }

    public ManagedUser withAccessResetAt(Instant performedAt) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                status,
                createdAt,
                inviteResentAt,
                performedAt);
    }

    public ManagedUser withStatus(UserStatus status) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                status,
                createdAt,
                inviteResentAt,
                accessResetAt);
    }

    public ManagedUser withIdentitySubject(String identitySubject) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                status,
                createdAt,
                inviteResentAt,
                accessResetAt);
    }
}

