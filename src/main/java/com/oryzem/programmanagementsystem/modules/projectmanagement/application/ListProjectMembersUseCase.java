package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectMembersUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectViewMapper viewMapper;

    public ListProjectMembersUseCase(ProjectAuthorizationService authorizationService, ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.viewMapper = viewMapper;
    }

    public List<ProjectReadModels.ProjectMemberReadModel> execute(String projectId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_PROJECT);
        return access.members().stream().map(viewMapper::toProjectMemberReadModel).toList();
    }
}


