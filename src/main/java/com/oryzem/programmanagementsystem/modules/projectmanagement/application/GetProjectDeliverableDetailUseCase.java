package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.ProjectReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetProjectDeliverableDetailUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectViewMapper viewMapper;

    public GetProjectDeliverableDetailUseCase(ProjectAuthorizationService authorizationService, ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.viewMapper = viewMapper;
    }

    public ProjectReadModels.ProjectDeliverableDetailReadModel execute(String projectId, String deliverableId, AuthenticatedUser actor) {
        ProjectAuthorizationService.DeliverableAccess access = authorizationService.authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.VIEW_DELIVERABLE);
        return viewMapper.toProjectDeliverableDetailReadModel(access.deliverable());
    }
}


