package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "identity_username", length = 255, nullable = false, unique = true)
    private String identityUsername;

    @Column(name = "identity_subject", length = 255, unique = true)
    private String identitySubject;

    @Column(name = "display_name", length = 160, nullable = false)
    private String displayName;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Role role;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", length = 32, nullable = false)
    private TenantType tenantType;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "invite_resent_at")
    private Instant inviteResentAt;

    @Column(name = "access_reset_at")
    private Instant accessResetAt;

    protected UserEntity() {
    }

    private UserEntity(
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
        this.id = id;
        this.identityUsername = identityUsername;
        this.identitySubject = identitySubject;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantType = tenantType;
        this.status = status;
        this.createdAt = createdAt;
        this.inviteResentAt = inviteResentAt;
        this.accessResetAt = accessResetAt;
    }

    public static UserEntity fromDomain(ManagedUser user) {
        return new UserEntity(
                user.id(),
                user.identityUsername(),
                user.identitySubject(),
                user.displayName(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.tenantType(),
                user.status(),
                user.createdAt(),
                user.inviteResentAt(),
                user.accessResetAt());
    }

    public ManagedUser toDomain() {
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

