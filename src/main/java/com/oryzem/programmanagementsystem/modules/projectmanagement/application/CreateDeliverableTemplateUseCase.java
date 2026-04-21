package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureArtifactType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDeliverableTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectMilestoneTemplateRepository milestoneTemplateRepository;
    private final DeliverableTemplateRepository deliverableTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public CreateDeliverableTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectPhaseTemplateRepository phaseTemplateRepository,
            ProjectMilestoneTemplateRepository milestoneTemplateRepository,
            DeliverableTemplateRepository deliverableTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.deliverableTemplateRepository = deliverableTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public TemplateViews.ProjectTemplateDeliverableTemplateView execute(String templateId, CreateDeliverableTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        administrationService.authorizeManagement(actor, template.ownerOrganizationId());
        ProjectPhaseTemplateAggregate phase = resolvePhaseTemplate(templateId, command.phaseTemplateId());
        ProjectMilestoneTemplateAggregate milestone = resolveMilestoneTemplate(templateId, command.milestoneTemplateId());
        validatePhaseMilestoneLink(phase, milestone);
        validateStructureLevel(template, command.appliesToType(), command.structureLevelTemplateId());
        List<DeliverableTemplateAggregate> existing = deliverableTemplateRepository.findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(templateId);
        if (existing.stream().anyMatch(item -> item.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("DELIVERABLE_TEMPLATE_CODE_ALREADY_EXISTS", "A deliverable template with this code already exists.");
        }
        DeliverableTemplateAggregate entity = deliverableTemplateRepository.save(new DeliverableTemplateAggregate(
                ProjectIds.newDeliverableTemplateId(),
                templateId,
                phase != null ? phase.id() : null,
                milestone != null ? milestone.id() : null,
                command.code().trim(),
                command.name().trim(),
                command.description(),
                command.deliverableType(),
                command.requiredDocument(),
                command.plannedDueOffsetDays(),
                command.appliesToType(),
                command.structureLevelTemplateId(),
                command.responsibleOrganizationRole(),
                command.approverOrganizationRole(),
                command.visibilityScope(),
                command.priority()));
        return viewMapper.toDeliverableTemplateView(entity);
    }

    private ProjectPhaseTemplateAggregate resolvePhaseTemplate(String templateId, String phaseTemplateId) {
        if (phaseTemplateId == null || phaseTemplateId.isBlank()) {
            return null;
        }
        return phaseTemplateRepository.findById(phaseTemplateId)
                .filter(phase -> phase.templateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectPhaseTemplate", phaseTemplateId));
    }

    private ProjectMilestoneTemplateAggregate resolveMilestoneTemplate(String templateId, String milestoneTemplateId) {
        if (milestoneTemplateId == null || milestoneTemplateId.isBlank()) {
            return null;
        }
        return milestoneTemplateRepository.findById(milestoneTemplateId)
                .filter(milestone -> milestone.templateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestoneTemplate", milestoneTemplateId));
    }

    private void validatePhaseMilestoneLink(ProjectPhaseTemplateAggregate phase, ProjectMilestoneTemplateAggregate milestone) {
        if (phase != null && milestone != null && milestone.phaseTemplateId() != null && !phase.id().equals(milestone.phaseTemplateId())) {
            throw new BusinessRuleException("DELIVERABLE_TEMPLATE_PHASE_MILESTONE_MISMATCH", "Deliverable milestone must belong to the selected phase.");
        }
    }

    private void validateStructureLevel(ProjectTemplateAggregate template, ProjectTemplateAppliesToType appliesToType, String structureLevelTemplateId) {
        ProjectStructureLevelTemplateAggregate level = structureLevelTemplateRepository.findById(structureLevelTemplateId)
                .filter(item -> item.structureTemplateId().equals(template.structureTemplateId()))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureLevelTemplate", structureLevelTemplateId));
        if (appliesToType == ProjectTemplateAppliesToType.ROOT_NODE && level.sequenceNo() != 1) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_ROOT_LEVEL_INVALID", "ROOT_NODE templates must target the root structure level.");
        }
        ProjectStructureRules.assertArtifactAllowed(toDefinition(level), ProjectStructureArtifactType.DELIVERABLE);
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    public record CreateDeliverableTemplateCommand(
            String phaseTemplateId,
            String milestoneTemplateId,
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            boolean requiredDocument,
            int plannedDueOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType responsibleOrganizationRole,
            ProjectOrganizationRoleType approverOrganizationRole,
            ProjectVisibilityScope visibilityScope,
            ProjectPriority priority) {
    }
}


