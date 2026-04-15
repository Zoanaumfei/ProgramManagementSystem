package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectStructureNodeAuthorizationSnapshot;
import org.springframework.stereotype.Component;

@Component
public class ProjectStructureNodeAccessPolicy {

    private final ProjectAccessPolicy projectAccessPolicy;
    private final ProjectVisibilityPolicy visibilityPolicy;

    public ProjectStructureNodeAccessPolicy(
            ProjectAccessPolicy projectAccessPolicy,
            ProjectVisibilityPolicy visibilityPolicy) {
        this.projectAccessPolicy = projectAccessPolicy;
        this.visibilityPolicy = visibilityPolicy;
    }

    public boolean isAllowed(ProjectStructureNodeAuthorizationSnapshot snapshot) {
        if (snapshot == null || snapshot.actorContext().actor() == null) {
            return false;
        }
        boolean visible = visibilityPolicy.canViewStructureNode(
                snapshot.leadOrganizationId(),
                snapshot.actorContext(),
                snapshot.visibilityScope(),
                snapshot.ownerOrganizationId(),
                snapshot.responsibleUserId());
        return switch (snapshot.permission()) {
            case VIEW_PROJECT -> visible;
            case EDIT_PROJECT -> visible
                    && projectAccessPolicy.isAllowed(new ProjectAuthorizationSnapshot(
                            snapshot.tenantId(),
                            snapshot.projectId(),
                            snapshot.leadOrganizationId(),
                            snapshot.visibilityScope(),
                            snapshot.actorContext(),
                            snapshot.permission()));
            case ADD_ORGANIZATION, ADD_MEMBER, VIEW_MILESTONE, EDIT_MILESTONE, VIEW_DELIVERABLE, EDIT_DELIVERABLE,
                    SUBMIT_DELIVERABLE, REVIEW_SUBMISSION, APPROVE_SUBMISSION, REJECT_SUBMISSION, VIEW_DOCUMENT,
                    UPLOAD_DOCUMENT, DOWNLOAD_DOCUMENT, DELETE_DOCUMENT -> false;
        };
    }
}
