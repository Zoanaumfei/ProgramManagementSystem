package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMilestoneUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateMilestoneUseCase(ProjectAuthorizationService authorizationService, ProjectMilestoneRepository milestoneRepository, ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.milestoneRepository = milestoneRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectMilestoneView execute(String projectId, String milestoneId, UpdateMilestoneCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.MilestoneAccess access = authorizationService.authorizeMilestone(projectId, milestoneId, actor, ProjectPermission.EDIT_MILESTONE);
        ProjectMilestoneAggregate milestone = access.milestone();
        if (command.version() != milestone.version()) {
            throw new ConflictException("Project milestone version mismatch.");
        }
        ProjectMilestoneAggregate updated = milestone.update(
                command.plannedDate() != null ? command.plannedDate() : milestone.plannedDate(),
                command.actualDate(),
                command.status() != null ? command.status() : milestone.status(),
                command.ownerOrganizationId() != null ? command.ownerOrganizationId() : milestone.ownerOrganizationId(),
                command.visibilityScope() != null ? command.visibilityScope() : milestone.visibilityScope());
        return viewMapper.toMilestoneView(milestoneRepository.save(updated));
    }

    public record UpdateMilestoneCommand(
            LocalDate plannedDate,
            LocalDate actualDate,
            ProjectMilestoneStatus status,
            String ownerOrganizationId,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }
}


