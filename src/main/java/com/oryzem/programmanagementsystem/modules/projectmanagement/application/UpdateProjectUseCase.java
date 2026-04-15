package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectViewMapper viewMapper;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public UpdateProjectUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectViewMapper viewMapper,
            ProjectRepository projectRepository,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.viewMapper = viewMapper;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional
    public ProjectViews.ProjectDetailView execute(String projectId, UpdateProjectCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.EDIT_PROJECT);
        if (command.version() != access.project().version()) {
            throw new ConflictException("Project version mismatch.");
        }
        ProjectAggregate updated = access.project().update(
                command.name() != null ? command.name() : access.project().name(),
                command.description() != null ? command.description() : access.project().description(),
                command.visibilityScope() != null ? command.visibilityScope() : access.project().visibilityScope(),
                command.plannedStartDate() != null ? command.plannedStartDate() : access.project().plannedStartDate(),
                command.plannedEndDate() != null ? command.plannedEndDate() : access.project().plannedEndDate(),
                command.status(),
                Instant.now(clock),
                LocalDate.now(clock));
        ProjectAggregate saved = projectRepository.save(updated);
        return viewMapper.toDetail(saved, access.organizations(), access.members());
    }

    public record UpdateProjectCommand(
            String name,
            String description,
            ProjectVisibilityScope visibilityScope,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            ProjectStatus status,
            long version) {
    }
}

