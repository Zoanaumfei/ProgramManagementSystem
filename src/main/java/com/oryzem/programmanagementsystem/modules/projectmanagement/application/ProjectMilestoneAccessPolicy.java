package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectMilestoneAuthorizationSnapshot;
import org.springframework.stereotype.Component;

@Component
public class ProjectMilestoneAccessPolicy {

    private final ProjectAccessPolicy projectAccessPolicy;
    private final ProjectVisibilityPolicy visibilityPolicy;

    public ProjectMilestoneAccessPolicy(
            ProjectAccessPolicy projectAccessPolicy,
            ProjectVisibilityPolicy visibilityPolicy) {
        this.projectAccessPolicy = projectAccessPolicy;
        this.visibilityPolicy = visibilityPolicy;
    }

    public boolean isAllowed(ProjectMilestoneAuthorizationSnapshot snapshot) {
        if (snapshot == null || snapshot.actorContext().actor() == null) {
            return false;
        }
        boolean visible = visibilityPolicy.canViewMilestone(
                snapshot.leadOrganizationId(),
                snapshot.actorContext(),
                snapshot.visibilityScope(),
                snapshot.ownerOrganizationId());
        return switch (snapshot.permission()) {
            case VIEW_MILESTONE -> visible;
            case EDIT_MILESTONE -> visible
                    && projectAccessPolicy.isAllowed(new ProjectAuthorizationSnapshot(
                            snapshot.tenantId(),
                            snapshot.projectId(),
                            snapshot.leadOrganizationId(),
                            snapshot.visibilityScope(),
                            snapshot.actorContext(),
                            snapshot.permission()));
            case VIEW_PROJECT, EDIT_PROJECT, ADD_ORGANIZATION, ADD_MEMBER, VIEW_DELIVERABLE, EDIT_DELIVERABLE,
                    SUBMIT_DELIVERABLE, REVIEW_SUBMISSION, APPROVE_SUBMISSION, REJECT_SUBMISSION, VIEW_DOCUMENT,
                    UPLOAD_DOCUMENT, DOWNLOAD_DOCUMENT, DELETE_DOCUMENT -> false;
        };
    }
}
