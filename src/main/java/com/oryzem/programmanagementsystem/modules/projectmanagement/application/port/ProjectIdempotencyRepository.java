package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.ProjectIdempotencyRecord;
import java.util.Optional;

public interface ProjectIdempotencyRepository {

    Optional<ProjectIdempotencyRecord> findByIdempotencyKey(String idempotencyKey, String tenantId, String operation);

    ProjectIdempotencyRecord save(ProjectIdempotencyRecord record);
}
