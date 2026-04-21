package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReorderProjectStructureLevelsUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public ReorderProjectStructureLevelsUseCase(
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
    public List<StructureViews.ProjectStructureLevelView> execute(String structureTemplateId, List<String> orderedLevelIds, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        var template = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        administrationService.authorizeManagement(actor, template.ownerOrganizationId());
        List<ProjectStructureLevelTemplateAggregate> existingLevels = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(structureTemplateId);
        if (orderedLevelIds == null || orderedLevelIds.isEmpty()) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_LEVEL_ORDER_INVALID", "Level order cannot be empty.");
        }
        if (existingLevels.size() != orderedLevelIds.size()
                || new HashSet<>(orderedLevelIds).size() != orderedLevelIds.size()
                || !new HashSet<>(orderedLevelIds).equals(existingLevels.stream().map(ProjectStructureLevelTemplateAggregate::id).collect(java.util.stream.Collectors.toSet()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_LEVEL_ORDER_INVALID", "Level order must include each structure level exactly once.");
        }
        Map<String, ProjectStructureLevelTemplateAggregate> levelsById = existingLevels.stream()
                .collect(java.util.stream.Collectors.toMap(ProjectStructureLevelTemplateAggregate::id, Function.identity()));
        int offset = existingLevels.size() + 10;
        existingLevels = existingLevels.stream()
                .map(level -> level.withSequence(level.sequenceNo() + offset))
                .toList();
        structureLevelTemplateRepository.saveAllAndFlush(existingLevels);
        for (int index = 0; index < orderedLevelIds.size(); index++) {
            levelsById.put(orderedLevelIds.get(index), levelsById.get(orderedLevelIds.get(index)).withSequence(index + 1));
        }
        return structureLevelTemplateRepository.saveAll(levelsById.values()).stream()
                .sorted(java.util.Comparator.comparingInt(ProjectStructureLevelTemplateAggregate::sequenceNo))
                .map(viewMapper::toStructureLevelView)
                .toList();
    }
}


