package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateMilestoneUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectViewMapper viewMapper;

    public CreateMilestoneUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectPhaseRepository phaseRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.structureNodeRepository = structureNodeRepository;
        this.phaseRepository = phaseRepository;
        this.milestoneRepository = milestoneRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectMilestoneView execute(String projectId, CreateMilestoneCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.EDIT_PROJECT);
        String structureNodeId = command.structureNodeId() != null ? command.structureNodeId() : ProjectIds.rootProjectStructureNodeId(projectId);
        ProjectStructureNodeAggregate node = structureNodeRepository.findByIdAndProjectId(structureNodeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureNode", structureNodeId));
        validateParticipantOrganization(access, command.ownerOrganizationId(), "PROJECT_MILESTONE_OWNER_ORGANIZATION_INVALID");
        String phaseId = resolvePhaseId(projectId, command.phaseId());
        List<ProjectMilestoneAggregate> nodeMilestones = milestoneRepository.findAllByProjectIdAndStructureNodeIdOrderBySequenceNoAsc(projectId, node.id());
        String code = requireText(command.code(), "code", "PROJECT_MILESTONE_FIELD_REQUIRED");
        if (nodeMilestones.stream().anyMatch(existing -> existing.code().equalsIgnoreCase(code))) {
            throw new BusinessRuleException(
                    "PROJECT_MILESTONE_CODE_ALREADY_EXISTS",
                    "Project milestone code already exists for this structure node.",
                    Map.of("code", code));
        }
        ProjectMilestoneAggregate milestone = milestoneRepository.save(new ProjectMilestoneAggregate(
                ProjectIds.newProjectMilestoneId(),
                projectId,
                node.id(),
                phaseId,
                code,
                requireText(command.name(), "name", "PROJECT_MILESTONE_FIELD_REQUIRED"),
                nodeMilestones.stream().mapToInt(ProjectMilestoneAggregate::sequence).max().orElse(0) + 1,
                command.plannedDate(),
                null,
                ProjectMilestoneStatus.NOT_STARTED,
                command.ownerOrganizationId(),
                command.visibilityScope() != null ? command.visibilityScope() : ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                0L));
        return viewMapper.toMilestoneView(milestone);
    }

    private String resolvePhaseId(String projectId, String phaseId) {
        if (phaseId == null || phaseId.isBlank()) {
            return null;
        }
        return phaseRepository.findByIdAndProjectId(phaseId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectPhase", phaseId))
                .id();
    }

    private void validateParticipantOrganization(ProjectAuthorizationService.ProjectAccess access, String organizationId, String businessCode) {
        if (organizationId != null && access.organizations().stream().noneMatch(organization -> organization.organizationId().equals(organizationId))) {
            throw new BusinessRuleException(businessCode, "Organization must belong to the project.");
        }
    }

    private String requireText(String value, String field, String businessCode) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(
                    businessCode,
                    "Project milestone field is required.",
                    Map.of("field", field));
        }
        return value.trim();
    }

    public record CreateMilestoneCommand(
            String structureNodeId,
            String phaseId,
            String code,
            String name,
            LocalDate plannedDate,
            String ownerOrganizationId,
            ProjectVisibilityScope visibilityScope) {
    }
}
