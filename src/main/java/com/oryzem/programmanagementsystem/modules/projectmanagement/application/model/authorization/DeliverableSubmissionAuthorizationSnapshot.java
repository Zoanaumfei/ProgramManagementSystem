package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;

public record DeliverableSubmissionAuthorizationSnapshot(
        ProjectDeliverableAuthorizationSnapshot deliverable,
        String submissionId,
        DeliverableSubmissionStatus status,
        ProjectPermission permission) {
}
