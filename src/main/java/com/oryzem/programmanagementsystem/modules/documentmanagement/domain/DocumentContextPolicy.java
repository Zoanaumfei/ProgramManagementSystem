package com.oryzem.programmanagementsystem.modules.documentmanagement.domain;

public record DocumentContextPolicy(
        boolean exists,
        boolean acceptsDocuments,
        boolean featureEnabled,
        String ownerOrganizationId,
        boolean canViewDocuments,
        boolean canUploadDocuments,
        boolean canDownloadDocuments,
        boolean canDeleteDocuments) {

    public boolean isAllowed(DocumentPermission permission) {
        return switch (permission) {
            case VIEW_DOCUMENT -> canViewDocuments;
            case UPLOAD_DOCUMENT -> canUploadDocuments;
            case DOWNLOAD_DOCUMENT -> canDownloadDocuments;
            case DELETE_DOCUMENT -> canDeleteDocuments;
        };
    }
}
