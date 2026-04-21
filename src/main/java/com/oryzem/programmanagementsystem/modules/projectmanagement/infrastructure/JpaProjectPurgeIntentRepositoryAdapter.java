package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectPurgeIntent;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeIntentRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectPurgeIntentRepositoryAdapter implements ProjectPurgeIntentRepository {

    private final SpringDataProjectPurgeIntentJpaRepository delegate;

    public JpaProjectPurgeIntentRepositoryAdapter(SpringDataProjectPurgeIntentJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ProjectPurgeIntent save(ProjectPurgeIntent intent) {
        ProjectPurgeIntentEntity saved = delegate.save(toEntity(intent));
        return toDomain(saved);
    }

    @Override
    public Optional<ProjectPurgeIntent> findByToken(String token) {
        return delegate.findById(token).map(this::toDomain);
    }

    private ProjectPurgeIntentEntity toEntity(ProjectPurgeIntent intent) {
        return new ProjectPurgeIntentEntity(
                intent.token(),
                intent.projectId(),
                intent.requestedByUserId(),
                intent.requestedByUsername(),
                intent.reason(),
                intent.status(),
                intent.createdAt(),
                intent.expiresAt(),
                intent.consumedAt());
    }

    private ProjectPurgeIntent toDomain(ProjectPurgeIntentEntity entity) {
        return new ProjectPurgeIntent(
                entity.getToken(),
                entity.getProjectId(),
                entity.getRequestedByUserId(),
                entity.getRequestedByUsername(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt());
    }
}
