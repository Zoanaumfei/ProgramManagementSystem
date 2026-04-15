package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoveProjectStructureNodeUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectViewMapper viewMapper;

    public MoveProjectStructureNodeUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.structureNodeRepository = structureNodeRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureNodeView execute(String projectId, String nodeId, MoveProjectStructureNodeCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.StructureNodeAccess access = authorizationService.authorizeStructureNode(projectId, nodeId, actor, ProjectPermission.EDIT_PROJECT);
        ProjectStructureNodeAggregate node = access.node();
        if (command.version() != node.version()) {
            throw new ConflictException("Project structure node version mismatch.");
        }
        if (node.parentNodeId() == null) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_ROOT_MOVE_NOT_ALLOWED", "Root structure nodes cannot be moved.");
        }
        ProjectStructureNodeAggregate newParent = structureNodeRepository.findByIdAndProjectId(command.newParentNodeId(), projectId)
                .orElseThrow(() -> new com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException("ProjectStructureNode", command.newParentNodeId()));
        if (node.id().equals(newParent.id())) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_SELF_PARENT_NOT_ALLOWED", "A structure node cannot be parent of itself.");
        }
        if (isDescendant(projectId, newParent.id(), node.id())) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_CYCLE_NOT_ALLOWED", "A structure node cannot be moved below one of its descendants.");
        }
        ProjectTemplateAggregate template = projectTemplateRepository.findById(access.project().templateId())
                .orElseThrow(() -> new IllegalStateException("Project template not found for project " + projectId));
        List<ProjectStructureLevelTemplateAggregate> levels = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(template.structureTemplateId());
        ProjectStructureLevelTemplateAggregate parentLevel = levels.stream().filter(level -> level.id().equals(newParent.levelTemplateId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Parent structure level not found."));
        ProjectStructureLevelTemplateAggregate nodeLevel = levels.stream().filter(level -> level.id().equals(node.levelTemplateId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Node structure level not found."));
        ProjectStructureRules.assertValidParentChild(toDefinition(parentLevel), toDefinition(nodeLevel));
        List<ProjectStructureNodeAggregate> siblings = structureNodeRepository.findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(projectId, newParent.id());
        if (siblings.stream().anyMatch(candidate -> !candidate.id().equals(node.id()) && candidate.code().equalsIgnoreCase(node.code()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_NODE_CODE_ALREADY_EXISTS", "A sibling structure node already uses this code.");
        }
        ProjectStructureNodeAggregate moved = node.moveTo(newParent.id(), siblings.size() + 1);
        return viewMapper.toStructureNodeView(structureNodeRepository.save(moved));
    }

    private boolean isDescendant(String projectId, String candidateNodeId, String ancestorNodeId) {
        ProjectStructureNodeAggregate current = structureNodeRepository.findByIdAndProjectId(candidateNodeId, projectId).orElse(null);
        while (current != null && current.parentNodeId() != null) {
            if (ancestorNodeId.equals(current.parentNodeId())) {
                return true;
            }
            current = structureNodeRepository.findByIdAndProjectId(current.parentNodeId(), projectId).orElse(null);
        }
        return false;
    }

    public record MoveProjectStructureNodeCommand(String newParentNodeId, long version) {
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }
}


