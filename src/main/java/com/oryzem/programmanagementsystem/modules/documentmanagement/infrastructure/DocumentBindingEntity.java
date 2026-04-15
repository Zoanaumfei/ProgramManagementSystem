package com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document_binding")
public class DocumentBindingEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "document_id", length = 64, nullable = false, unique = true)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 64, nullable = false)
    private DocumentContextType contextType;

    @Column(name = "context_id", length = 128, nullable = false)
    private String contextId;

    @Column(name = "owner_organization_id", length = 64, nullable = false)
    private String ownerOrganizationId;

    @Column(name = "created_by_user_id", length = 64, nullable = false)
    private String createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentBindingEntity() {
    }

    public static DocumentBindingEntity create(
            String id,
            String documentId,
            DocumentContextType contextType,
            String contextId,
            String ownerOrganizationId,
            String createdByUserId,
            Instant createdAt) {
        DocumentBindingEntity entity = new DocumentBindingEntity();
        entity.id = id;
        entity.documentId = documentId;
        entity.contextType = contextType;
        entity.contextId = contextId;
        entity.ownerOrganizationId = ownerOrganizationId;
        entity.createdByUserId = createdByUserId;
        entity.createdAt = createdAt;
        return entity;
    }

    public String getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public DocumentContextType getContextType() {
        return contextType;
    }

    public String getContextId() {
        return contextId;
    }

    public String getOwnerOrganizationId() {
        return ownerOrganizationId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
