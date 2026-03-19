package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.time.Instant;

public record ManagedUser(
        String id,
        String identityUsername,
        String identitySubject,
        String displayName,
        String email,
        Role role,
        String tenantId,
        TenantType tenantType,
        UserStatus status,
        Instant createdAt,
        Instant inviteResentAt,
        Instant accessResetAt) {

    public ManagedUser withUpdatedDetails(
            String displayName,
            String email,
            Role role,
            String tenantId,
            TenantType tenantType) {
        return new ManagedUser(
                id,
                identityUsername,
                identitySubject,
                displayName,
                email,
                role,
                tenantId,
                tenantType,
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
                role,
                tenantId,
                tenantType,
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
                role,
                tenantId,
                tenantType,
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
                role,
                tenantId,
                tenantType,
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
                role,
                tenantId,
                tenantType,
                status,
                createdAt,
                inviteResentAt,
                accessResetAt);
    }
}
