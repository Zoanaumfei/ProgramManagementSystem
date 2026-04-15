package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.TemplateReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetProjectStructureTemplateDetailUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectMilestoneTemplateRepository milestoneTemplateRepository;
    private final DeliverableTemplateRepository deliverableTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public GetProjectStructureTemplateDetailUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectMilestoneTemplateRepository milestoneTemplateRepository,
            DeliverableTemplateRepository deliverableTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.deliverableTemplateRepository = deliverableTemplateRepository;
        this.viewMapper = viewMapper;
    }

    public StructureReadModels.ProjectStructureTemplateDetailReadModel execute(String structureTemplateId, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        if (actor == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated user is required.");
        }
        ProjectStructureTemplateAggregate structureTemplate = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        List<ProjectTemplateAggregate> projectTemplates = projectTemplateRepository.findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(structureTemplateId);
        List<String> projectTemplateIds = projectTemplates.stream().map(ProjectTemplateAggregate::id).toList();
        List<TemplateReadModels.ProjectMilestoneTemplateReadModel> milestoneTemplates = projectTemplateIds.isEmpty()
                ? List.of()
                : milestoneTemplateRepository.findAllByTemplateIdInOrderByTemplateIdAscSequenceNoAsc(projectTemplateIds).stream()
                        .map(viewMapper::toProjectMilestoneTemplateReadModel)
                        .toList();
        List<TemplateReadModels.ProjectDeliverableTemplateReadModel> deliverableTemplates = projectTemplateIds.isEmpty()
                ? List.of()
                : deliverableTemplateRepository.findAllByTemplateIdInOrderByTemplateIdAscPlannedDueOffsetDaysAscCodeAsc(projectTemplateIds).stream()
                        .map(viewMapper::toProjectDeliverableTemplateReadModel)
                        .toList();
        return new StructureReadModels.ProjectStructureTemplateDetailReadModel(
                structureTemplate.id(),
                structureTemplate.name(),
                structureTemplate.frameworkType(),
                structureTemplate.version(),
                structureTemplate.active(),
                structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(structureTemplateId).stream()
                        .map(viewMapper::toProjectStructureLevelReadModel)
                        .toList(),
                projectTemplates.stream().map(viewMapper::toProjectTemplateListReadModel).toList(),
                milestoneTemplates,
                deliverableTemplates);
    }
}



