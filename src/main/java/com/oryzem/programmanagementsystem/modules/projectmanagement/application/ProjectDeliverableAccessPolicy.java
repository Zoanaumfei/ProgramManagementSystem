package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectDeliverableAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import org.springframework.stereotype.Component;

@Component
public class ProjectDeliverableAccessPolicy {

    private final ProjectVisibilityPolicy visibilityPolicy;

    public ProjectDeliverableAccessPolicy(ProjectVisibilityPolicy visibilityPolicy) {
        this.visibilityPolicy = visibilityPolicy;
    }

    public boolean isAllowed(ProjectDeliverableAuthorizationSnapshot snapshot) {
        ProjectActorContext actorContext = snapshot.actorContext();
        boolean responsible = actorContext.matchesAssignment(snapshot.responsibleOrganizationId(), snapshot.responsibleUserId());
        boolean approver = actorContext.matchesAssignment(snapshot.approverOrganizationId(), snapshot.approverUserId());
        boolean visible = visibilityPolicy.canViewDeliverable(
                snapshot.leadOrganizationId(),
                actorContext,
                snapshot.visibilityScope(),
                snapshot.responsibleOrganizationId(),
                snapshot.responsibleUserId(),
                snapshot.approverOrganizationId(),
                snapshot.approverUserId());
        return switch (snapshot.permission()) {
            case VIEW_DELIVERABLE, VIEW_DOCUMENT, DOWNLOAD_DOCUMENT -> visible;
            case EDIT_DELIVERABLE, UPLOAD_DOCUMENT, DELETE_DOCUMENT -> actorContext.manager() || responsible;
            case SUBMIT_DELIVERABLE -> visible && (actorContext.manager() || responsible)
                    && snapshot.status() != ProjectDeliverableStatus.APPROVED
                    && snapshot.status() != ProjectDeliverableStatus.WAIVED;
            case REVIEW_SUBMISSION, APPROVE_SUBMISSION, REJECT_SUBMISSION -> visible && (actorContext.manager() || approver);
            case VIEW_PROJECT, EDIT_PROJECT, ADD_ORGANIZATION, ADD_MEMBER, VIEW_MILESTONE, EDIT_MILESTONE -> false;
        };
    }
}
