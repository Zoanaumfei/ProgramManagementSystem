package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectStructureNodeUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateProjectStructureNodeUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.structureNodeRepository = structureNodeRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureNodeView execute(String projectId, String nodeId, UpdateProjectStructureNodeCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.StructureNodeAccess access = authorizationService.authorizeStructureNode(projectId, nodeId, actor, ProjectPermission.EDIT_PROJECT);
        ProjectStructureNodeAggregate node = access.node();
        if (command.version() != node.version()) {
            throw new ConflictException("Project structure node version mismatch.");
        }
        if (command.ownerOrganizationId() != null
                && access.organizations().stream().noneMatch(org -> org.organizationId().equals(command.ownerOrganizationId()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_OWNER_ORGANIZATION_INVALID", "Structure node owner organization must belong to the project.");
        }
        List<ProjectStructureNodeAggregate> siblings = node.parentNodeId() == null
                ? structureNodeRepository.findAllByProjectIdOrderBySequenceNoAscIdAsc(projectId).stream().filter(candidate -> candidate.parentNodeId() == null).toList()
                : structureNodeRepository.findAllByProjectIdAndParentNodeIdOrderBySequenceNoAscIdAsc(projectId, node.parentNodeId());
        if (siblings.stream().anyMatch(candidate -> !candidate.id().equals(node.id()) && candidate.code().equalsIgnoreCase(command.code().trim()))) {
            throw new BusinessRuleException("PROJECT_STRUCTURE_NODE_CODE_ALREADY_EXISTS", "A sibling structure node already uses this code.");
        }
        ProjectStructureNodeAggregate updated = node.update(
                command.name().trim(),
                command.code().trim(),
                command.ownerOrganizationId() != null ? command.ownerOrganizationId() : node.ownerOrganizationId(),
                command.responsibleUserId(),
                command.visibilityScope() != null ? command.visibilityScope() : node.visibilityScope());
        return viewMapper.toStructureNodeView(structureNodeRepository.save(updated));
    }

    public record UpdateProjectStructureNodeCommand(
            String name,
            String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }
}


