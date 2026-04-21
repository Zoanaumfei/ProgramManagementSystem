package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectStructureLevelTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public CreateProjectStructureLevelTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureLevelView execute(String structureTemplateId, CreateProjectStructureLevelTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        var template = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        administrationService.authorizeManagement(actor, template.ownerOrganizationId());
        List<ProjectStructureLevelTemplateAggregate> existingLevels = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(structureTemplateId);
        if (existingLevels.stream().anyMatch(level -> level.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_LEVEL_CODE_ALREADY_EXISTS", "A structure level with this code already exists in the template.");
        }
        ProjectStructureRules.validateEditableTemplate(candidateLevels(existingLevels, command));
        ProjectStructureLevelTemplateAggregate entity = structureLevelTemplateRepository.save(new ProjectStructureLevelTemplateAggregate(
                ProjectIds.newProjectStructureLevelTemplateId(),
                structureTemplateId,
                existingLevels.size() + 1,
                command.name().trim(),
                command.code().trim(),
                command.allowsChildren(),
                command.allowsMilestones(),
                command.allowsDeliverables()));
        return viewMapper.toStructureLevelView(entity);
    }

    private java.util.List<ProjectStructureLevelDefinition> candidateLevels(
            java.util.List<ProjectStructureLevelTemplateAggregate> existingLevels,
            CreateProjectStructureLevelTemplateCommand command) {
        java.util.List<ProjectStructureLevelDefinition> levels = new java.util.ArrayList<>(existingLevels.stream()
                .map(this::toDefinition)
                .toList());
        levels.add(new ProjectStructureLevelDefinition(
                "candidate",
                existingLevels.size() + 1,
                command.allowsChildren(),
                command.allowsMilestones(),
                command.allowsDeliverables()));
        return levels;
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    public record CreateProjectStructureLevelTemplateCommand(
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }
}


