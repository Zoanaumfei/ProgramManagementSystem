package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import org.springframework.stereotype.Component;

@Component
public class ProjectAccessPolicy {

    private final ProjectVisibilityPolicy visibilityPolicy;

    public ProjectAccessPolicy(ProjectVisibilityPolicy visibilityPolicy) {
        this.visibilityPolicy = visibilityPolicy;
    }

    public boolean isAllowed(ProjectAuthorizationSnapshot snapshot) {
        ProjectActorContext actorContext = snapshot.actorContext();
        boolean member = !actorContext.memberRoles().isEmpty();
        return switch (snapshot.permission()) {
            case VIEW_PROJECT, VIEW_MILESTONE, VIEW_DOCUMENT, DOWNLOAD_DOCUMENT -> actorContext.organizationParticipant() || member;
            case EDIT_PROJECT, ADD_ORGANIZATION, ADD_MEMBER, EDIT_MILESTONE, UPLOAD_DOCUMENT, DELETE_DOCUMENT -> actorContext.manager();
            case VIEW_DELIVERABLE, EDIT_DELIVERABLE, SUBMIT_DELIVERABLE, REVIEW_SUBMISSION, APPROVE_SUBMISSION, REJECT_SUBMISSION -> actorContext.organizationParticipant() || member;
        };
    }
}
