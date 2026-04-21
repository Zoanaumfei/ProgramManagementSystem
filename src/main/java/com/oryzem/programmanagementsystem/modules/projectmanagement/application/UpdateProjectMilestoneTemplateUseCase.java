package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureArtifactType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectMilestoneTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectMilestoneTemplateRepository milestoneTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateProjectMilestoneTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectPhaseTemplateRepository phaseTemplateRepository,
            ProjectMilestoneTemplateRepository milestoneTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public TemplateViews.ProjectTemplateMilestoneTemplateView execute(String templateId, String milestoneTemplateId, UpdateProjectMilestoneTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        administrationService.authorizeManagement(actor, template.ownerOrganizationId());
        ProjectMilestoneTemplateAggregate entity = milestoneTemplateRepository.findById(milestoneTemplateId)
                .filter(item -> item.templateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestoneTemplate", milestoneTemplateId));
        resolvePhaseTemplate(templateId, command.phaseTemplateId());
        validateStructureLevel(template, command.appliesToType(), command.structureLevelTemplateId());
        List<ProjectMilestoneTemplateAggregate> existing = milestoneTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(templateId);
        if (existing.stream().anyMatch(item -> !item.id().equals(milestoneTemplateId) && item.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("PROJECT_MILESTONE_TEMPLATE_CODE_ALREADY_EXISTS", "A milestone template with this code already exists.");
        }
        entity = entity.update(
                command.phaseTemplateId(),
                command.code().trim(),
                command.name().trim(),
                command.description(),
                command.plannedOffsetDays(),
                command.appliesToType(),
                command.structureLevelTemplateId(),
                command.ownerOrganizationRole(),
                command.visibilityScope());
        entity = milestoneTemplateRepository.save(entity);
        return viewMapper.toMilestoneTemplateView(entity);
    }

    private void resolvePhaseTemplate(String templateId, String phaseTemplateId) {
        if (phaseTemplateId == null || phaseTemplateId.isBlank()) {
            return;
        }
        phaseTemplateRepository.findById(phaseTemplateId)
                .filter(phase -> phase.templateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectPhaseTemplate", phaseTemplateId));
    }

    private void validateStructureLevel(ProjectTemplateAggregate template, ProjectTemplateAppliesToType appliesToType, String structureLevelTemplateId) {
        ProjectStructureLevelTemplateAggregate level = structureLevelTemplateRepository.findById(structureLevelTemplateId)
                .filter(item -> item.structureTemplateId().equals(template.structureTemplateId()))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureLevelTemplate", structureLevelTemplateId));
        if (appliesToType == ProjectTemplateAppliesToType.ROOT_NODE && level.sequenceNo() != 1) {
            throw new BusinessRuleException("PROJECT_TEMPLATE_ROOT_LEVEL_INVALID", "ROOT_NODE templates must target the root structure level.");
        }
        ProjectStructureRules.assertArtifactAllowed(toDefinition(level), ProjectStructureArtifactType.MILESTONE);
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    public record UpdateProjectMilestoneTemplateCommand(
            String phaseTemplateId,
            String code,
            String name,
            String description,
            int plannedOffsetDays,
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectOrganizationRoleType ownerOrganizationRole,
            ProjectVisibilityScope visibilityScope) {
    }
}


