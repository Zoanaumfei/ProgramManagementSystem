package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectPurgeIntent;
import java.util.Optional;

public interface ProjectPurgeIntentRepository {

    ProjectPurgeIntent save(ProjectPurgeIntent intent);

    Optional<ProjectPurgeIntent> findByToken(String token);
}
