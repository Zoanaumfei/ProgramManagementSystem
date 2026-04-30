package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDeliverableUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectViewMapper viewMapper;

    public CreateDeliverableUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectPhaseRepository phaseRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectDeliverableRepository deliverableRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.structureNodeRepository = structureNodeRepository;
        this.phaseRepository = phaseRepository;
        this.milestoneRepository = milestoneRepository;
        this.deliverableRepository = deliverableRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectDeliverableView execute(String projectId, CreateDeliverableCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.EDIT_PROJECT);
        String structureNodeId = command.structureNodeId() != null ? command.structureNodeId() : ProjectIds.rootProjectStructureNodeId(projectId);
        ProjectStructureNodeAggregate node = structureNodeRepository.findByIdAndProjectId(structureNodeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureNode", structureNodeId));
        validateParticipantOrganization(access, command.responsibleOrganizationId(), "PROJECT_DELIVERABLE_RESPONSIBLE_ORGANIZATION_INVALID");
        validateParticipantOrganization(access, command.approverOrganizationId(), "PROJECT_DELIVERABLE_APPROVER_ORGANIZATION_INVALID");
        String phaseId = resolvePhaseId(projectId, command.phaseId());
        String milestoneId = resolveMilestoneId(projectId, node.id(), command.milestoneId());
        String code = requireText(command.code(), "code");
        if (deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(projectId, node.id()).stream()
                .anyMatch(existing -> existing.code().equalsIgnoreCase(code))) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_CODE_ALREADY_EXISTS",
                    "Project deliverable code already exists for this structure node.",
                    Map.of("code", code));
        }
        ProjectDeliverableAggregate deliverable = deliverableRepository.save(new ProjectDeliverableAggregate(
                ProjectIds.newDeliverableId(),
                projectId,
                node.id(),
                phaseId,
                milestoneId,
                code,
                requireText(command.name(), "name"),
                command.description(),
                command.deliverableType() != null ? command.deliverableType() : DeliverableType.DOCUMENT_PACKAGE,
                command.responsibleOrganizationId(),
                command.responsibleUserId(),
                command.approverOrganizationId(),
                command.approverUserId(),
                command.requiredDocument() != null && command.requiredDocument(),
                command.plannedDueDate(),
                null,
                null,
                ProjectDeliverableStatus.NOT_STARTED,
                command.priority() != null ? command.priority() : ProjectPriority.MEDIUM,
                command.visibilityScope() != null ? command.visibilityScope() : ProjectVisibilityScope.ALL_PROJECT_PARTICIPANTS,
                0L));
        return viewMapper.toDeliverableView(deliverable);
    }

    private String resolvePhaseId(String projectId, String phaseId) {
        if (phaseId == null || phaseId.isBlank()) {
            return null;
        }
        return phaseRepository.findByIdAndProjectId(phaseId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectPhase", phaseId))
                .id();
    }

    private String resolveMilestoneId(String projectId, String structureNodeId, String milestoneId) {
        if (milestoneId == null || milestoneId.isBlank()) {
            return null;
        }
        ProjectMilestoneAggregate milestone = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));
        if (!structureNodeId.equals(milestone.structureNodeId())) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_MILESTONE_NODE_MISMATCH",
                    "Deliverable milestone must belong to the selected structure node.");
        }
        return milestone.id();
    }

    private void validateParticipantOrganization(ProjectAuthorizationService.ProjectAccess access, String organizationId, String businessCode) {
        if (organizationId != null && access.organizations().stream().noneMatch(organization -> organization.organizationId().equals(organizationId))) {
            throw new BusinessRuleException(businessCode, "Organization must belong to the project.");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_FIELD_REQUIRED",
                    "Project deliverable field is required.",
                    Map.of("field", field));
        }
        return value.trim();
    }

    public record CreateDeliverableCommand(
            String structureNodeId,
            String phaseId,
            String milestoneId,
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            String responsibleOrganizationId,
            String responsibleUserId,
            String approverOrganizationId,
            String approverUserId,
            Boolean requiredDocument,
            LocalDate plannedDueDate,
            ProjectPriority priority,
            ProjectVisibilityScope visibilityScope) {
    }
}
