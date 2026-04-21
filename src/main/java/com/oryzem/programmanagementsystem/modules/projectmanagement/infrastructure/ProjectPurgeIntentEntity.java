package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectPurgeIntentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_purge_intent")
public class ProjectPurgeIntentEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String token;

    @Column(name = "project_id", length = 64, nullable = false)
    private String projectId;

    @Column(name = "requested_by_user_id", length = 64)
    private String requestedByUserId;

    @Column(name = "requested_by_username", length = 160)
    private String requestedByUsername;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ProjectPurgeIntentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected ProjectPurgeIntentEntity() {
    }

    public ProjectPurgeIntentEntity(
            String token,
            String projectId,
            String requestedByUserId,
            String requestedByUsername,
            String reason,
            ProjectPurgeIntentStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant consumedAt) {
        this.token = token;
        this.projectId = projectId;
        this.requestedByUserId = requestedByUserId;
        this.requestedByUsername = requestedByUsername;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
    }

    public String getToken() {
        return token;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public String getReason() {
        return reason;
    }

    public ProjectPurgeIntentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
