package com.oryzem.programmanagementsystem.modules.documentmanagement.application.model;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import java.time.Instant;

public record DocumentBindingRecord(
        String id,
        String documentId,
        DocumentContextType contextType,
        String contextId,
        String ownerOrganizationId,
        String createdByUserId,
        Instant createdAt) {

    public static DocumentBindingRecord create(
            String id,
            String documentId,
            DocumentContextType contextType,
            String contextId,
            String ownerOrganizationId,
            String createdByUserId,
            Instant createdAt) {
        return new DocumentBindingRecord(
                id,
                documentId,
                contextType,
                contextId,
                ownerOrganizationId,
                createdByUserId,
                createdAt);
    }
}