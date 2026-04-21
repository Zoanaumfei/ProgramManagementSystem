package com.oryzem.programmanagementsystem.modules.projectmanagement.application.port;

public interface ProjectPurgeArtifactPort {

    ProjectPurgePlan loadPlan(String projectId);

    void purgeArtifacts(ProjectPurgePlan plan);

    record ProjectPurgePlan(
            String projectId,
            long organizationCount,
            long memberCount,
            long phaseCount,
            long milestoneCount,
            long deliverableCount,
            long submissionCount,
            long submissionDocumentLinkCount,
            long structureNodeCount,
            java.util.List<String> deliverableIds,
            java.util.List<String> submissionIds) {
    }
}
