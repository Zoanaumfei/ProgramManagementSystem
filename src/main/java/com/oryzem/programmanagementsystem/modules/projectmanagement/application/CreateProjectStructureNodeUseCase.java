package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectStructureNodeUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectTemplateInstantiationService instantiationService;
    private final ProjectViewMapper viewMapper;

    public CreateProjectStructureNodeUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectTemplateInstantiationService instantiationService,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.structureNodeRepository = structureNodeRepository;
        this.instantiationService = instantiationService;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureNodeView execute(String projectId, CreateProjectStructureNodeCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.EDIT_PROJECT);
        ProjectStructureNodeAggregate parent = structureNodeRepository.findByIdAndProjectId(command.parentNodeId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureNode", command.parentNodeId()));
        ProjectTemplateAggregate template = projectTemplateRepository.findById(access.project().templateId())
                .orElseThrow(() -> new IllegalStateException("Project template not found for project " + projectId));
        List<ProjectStructureLevelTemplateAggregate> levels = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(template.structureTemplateId());
        ProjectStructureLevelTemplateAggregate parentLevel = levels.stream()
                .filter(level -> level.id().equals(parent.levelTemplateId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Parent structure level not found."));
        ProjectStructureLevelTemplateAggregate childLevel = levels.stream()
                .filter(level -> level.id().equals(ProjectStructureRules.childLevelOf(
                        toDefinition(parentLevel),
                        levels.stream().map(this::toDefinition).toList()).id()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("PROJECT_STRUCTURE_LEVEL_NOT_AVAILABLE", "No child structure level is configured after the parent level."));
        if (command.ownerOrganizationId() != null
                && access.organizations().stream().noneMatch(org -> org.organizationId().equals(command.ownerOrganizationId()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_OWNER_ORGANIZATION_INVALID", "Structure node owner organization must belong to the project.");
        }
        List<ProjectStructureNodeAggregate> siblings = structureNodeRepository.findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(projectId, parent.id());
        if (siblings.stream().anyMatch(node -> node.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_NODE_CODE_ALREADY_EXISTS", "A sibling structure node already uses this code.");
        }
        ProjectStructureNodeAggregate node = structureNodeRepository.save(new ProjectStructureNodeAggregate(
                ProjectIds.newProjectStructureNodeId(),
                projectId,
                childLevel.id(),
                parent.id(),
                command.name().trim(),
                command.code().trim(),
                siblings.size() + 1,
                command.ownerOrganizationId() != null ? command.ownerOrganizationId() : parent.ownerOrganizationId(),
                command.responsibleUserId(),
                ProjectStructureNodeStatus.PLANNED,
                ProjectStructureRules.inheritedVisibility(parent.visibilityScope(), command.visibilityScope()),
                0L));
        instantiationService.instantiateForNode(access.project(), node, access.organizations());
        return viewMapper.toStructureNodeView(node);
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    public record CreateProjectStructureNodeCommand(
            String parentNodeId,
            String name,
            String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectVisibilityScope visibilityScope) {
    }
}


