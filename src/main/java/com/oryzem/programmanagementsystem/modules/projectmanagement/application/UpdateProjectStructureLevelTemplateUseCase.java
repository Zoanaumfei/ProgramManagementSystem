package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectStructureLevelTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateProjectStructureLevelTemplateUseCase(
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
    public StructureViews.ProjectStructureLevelView execute(String structureTemplateId, String levelTemplateId, UpdateProjectStructureLevelTemplateCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        ProjectStructureLevelTemplateAggregate entity = structureLevelTemplateRepository.findById(levelTemplateId)
                .filter(level -> level.structureTemplateId().equals(structureTemplateId))
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureLevelTemplate", levelTemplateId));
        List<ProjectStructureLevelTemplateAggregate> siblings = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(structureTemplateId);
        if (siblings.stream().anyMatch(level -> !level.id().equals(levelTemplateId) && level.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_LEVEL_CODE_ALREADY_EXISTS", "A structure level with this code already exists in the template.");
        }
        ProjectStructureRules.validateEditableTemplate(siblings.stream()
                .map(level -> level.id().equals(levelTemplateId)
                        ? new ProjectStructureLevelDefinition(
                                level.id(),
                                level.sequenceNo(),
                                command.allowsChildren(),
                                command.allowsMilestones(),
                                command.allowsDeliverables())
                        : toDefinition(level))
                .toList());
        entity = entity.update(
                command.name().trim(),
                command.code().trim(),
                command.allowsChildren(),
                command.allowsMilestones(),
                command.allowsDeliverables());
        structureLevelTemplateRepository.save(entity);
        return viewMapper.toStructureLevelView(entity);
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    public record UpdateProjectStructureLevelTemplateCommand(
            String name,
            String code,
            boolean allowsChildren,
            boolean allowsMilestones,
            boolean allowsDeliverables) {
    }
}


