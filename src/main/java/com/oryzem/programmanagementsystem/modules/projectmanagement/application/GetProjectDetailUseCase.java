package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetProjectDetailUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectViewMapper viewMapper;

    public GetProjectDetailUseCase(ProjectAuthorizationService authorizationService, ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.viewMapper = viewMapper;
    }

    public ProjectReadModels.ProjectDetailReadModel execute(String projectId, AuthenticatedUser actor) {
        ProjectAuthorizationService.ProjectAccess access = authorizationService.authorizeProject(projectId, actor, ProjectPermission.VIEW_PROJECT);
        return viewMapper.toProjectDetailReadModel(access.project(), access.organizations(), access.members());
    }
}


