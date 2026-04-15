package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.DeliverableSubmissionAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import org.springframework.stereotype.Component;

@Component
public class DeliverableSubmissionAccessPolicy {

    private final ProjectDeliverableAccessPolicy deliverableAccessPolicy;

    public DeliverableSubmissionAccessPolicy(ProjectDeliverableAccessPolicy deliverableAccessPolicy) {
        this.deliverableAccessPolicy = deliverableAccessPolicy;
    }

    public boolean isAllowed(DeliverableSubmissionAuthorizationSnapshot snapshot) {
        if (!deliverableAccessPolicy.isAllowed(new com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectDeliverableAuthorizationSnapshot(
                snapshot.deliverable().tenantId(),
                snapshot.deliverable().projectId(),
                snapshot.deliverable().deliverableId(),
                snapshot.deliverable().leadOrganizationId(),
                snapshot.deliverable().visibilityScope(),
                snapshot.deliverable().responsibleOrganizationId(),
                snapshot.deliverable().responsibleUserId(),
                snapshot.deliverable().approverOrganizationId(),
                snapshot.deliverable().approverUserId(),
                snapshot.deliverable().status(),
                snapshot.deliverable().actorContext(),
                mapSubmissionPermission(snapshot.permission())))) {
            return false;
        }
        if (snapshot.permission() == ProjectPermission.UPLOAD_DOCUMENT) {
            return snapshot.status() == DeliverableSubmissionStatus.SUBMITTED
                    || snapshot.status() == DeliverableSubmissionStatus.UNDER_REVIEW;
        }
        return true;
    }

    private ProjectPermission mapSubmissionPermission(ProjectPermission permission) {
        return switch (permission) {
            case VIEW_DOCUMENT, DOWNLOAD_DOCUMENT -> ProjectPermission.VIEW_DELIVERABLE;
            case UPLOAD_DOCUMENT, DELETE_DOCUMENT -> ProjectPermission.SUBMIT_DELIVERABLE;
            case REVIEW_SUBMISSION, APPROVE_SUBMISSION, REJECT_SUBMISSION -> permission;
            default -> permission;
        };
    }
}
