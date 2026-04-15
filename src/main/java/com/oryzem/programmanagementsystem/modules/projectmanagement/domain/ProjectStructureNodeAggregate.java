package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public record ProjectStructureNodeAggregate(
        String id,
        String projectId,
        String levelTemplateId,
        String parentNodeId,
        String name,
        String code,
        int sequenceNo,
        String ownerOrganizationId,
        String responsibleUserId,
        ProjectStructureNodeStatus status,
        ProjectVisibilityScope visibilityScope,
        long version) {

    public static ProjectStructureNodeAggregate createRoot(
            String id,
            String projectId,
            String levelTemplateId,
            String name,
            String code,
            String ownerOrganizationId,
            String responsibleUserId,
            ProjectStructureNodeStatus status,
            ProjectVisibilityScope visibilityScope) {
        return new ProjectStructureNodeAggregate(
                id,
                projectId,
                levelTemplateId,
                null,
                name,
                code,
                1,
                ownerOrganizationId,
                responsibleUserId,
                status,
                visibilityScope,
                0L);
    }

    public ProjectStructureNodeAggregate update(
            String nextName,
            String nextCode,
            String nextOwnerOrganizationId,
            String nextResponsibleUserId,
            ProjectVisibilityScope nextVisibilityScope) {
        return new ProjectStructureNodeAggregate(
                id,
                projectId,
                levelTemplateId,
                parentNodeId,
                nextName,
                nextCode,
                sequenceNo,
                nextOwnerOrganizationId,
                nextResponsibleUserId,
                status,
                nextVisibilityScope,
                version);
    }

    public ProjectStructureNodeAggregate moveTo(String nextParentNodeId, int nextSequenceNo) {
        return new ProjectStructureNodeAggregate(
                id,
                projectId,
                levelTemplateId,
                nextParentNodeId,
                name,
                code,
                nextSequenceNo,
                ownerOrganizationId,
                responsibleUserId,
                status,
                visibilityScope,
                version);
    }
}
