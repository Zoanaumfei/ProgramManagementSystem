package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetProjectStructureTreeUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectViewMapper viewMapper;

    public GetProjectStructureTreeUseCase(
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

    public StructureReadModels.ProjectStructureTreeReadModel execute(String projectId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_PROJECT);
        ProjectTemplateAggregate template = projectTemplateRepository.findById(access.project().templateId())
                .orElseThrow(() -> new IllegalStateException("Project template not found for project " + projectId));
        List<ProjectStructureNodeAggregate> visibleNodes = filterVisibleNodes(
                structureNodeRepository.findAllByProjectIdOrderBySequenceNoAscIdAsc(projectId),
                access,
                actor);
        return new StructureReadModels.ProjectStructureTreeReadModel(
                projectId,
                structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(template.structureTemplateId()).stream()
                        .map(viewMapper::toProjectStructureLevelReadModel)
                        .toList(),
                visibleNodes.stream().map(viewMapper::toProjectStructureNodeReadModel)
                        .toList());
    }

    private List<ProjectStructureNodeAggregate> filterVisibleNodes(
            List<ProjectStructureNodeAggregate> nodes,
            ProjectAuthorizationService.ProjectAccess access,
            AuthenticatedUser actor) {
        Map<String, List<ProjectStructureNodeAggregate>> childrenByParent = nodes.stream()
                .filter(node -> node.parentNodeId() != null)
                .collect(Collectors.groupingBy(
                        ProjectStructureNodeAggregate::parentNodeId,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));
        List<ProjectStructureNodeAggregate> visibleNodes = new ArrayList<>();
        nodes.stream()
                .filter(node -> node.parentNodeId() == null)
                .forEach(root -> collectVisibleSubtree(root, childrenByParent, visibleNodes, access, actor));
        return visibleNodes;
    }

    private void collectVisibleSubtree(
            ProjectStructureNodeAggregate node,
            Map<String, List<ProjectStructureNodeAggregate>> childrenByParent,
            List<ProjectStructureNodeAggregate> visibleNodes,
            ProjectAuthorizationService.ProjectAccess access,
            AuthenticatedUser actor) {
        if (!authorizationService.canAccessStructureNode(
                access.project(),
                access.organizations(),
                access.members(),
                node,
                actor,
                ProjectPermission.VIEW_PROJECT)) {
            return;
        }
        visibleNodes.add(node);
        childrenByParent.getOrDefault(node.id(), List.of())
                .forEach(child -> collectVisibleSubtree(child, childrenByParent, visibleNodes, access, actor));
    }
}



