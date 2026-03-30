package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "organization_relationship")
class OrganizationRelationshipEntity extends JpaAuditableEntity {

    private String sourceOrganizationId;
    private String targetOrganizationId;
    private OrganizationRelationshipType relationshipType;
    private OrganizationRelationshipStatus status;

    protected OrganizationRelationshipEntity() {
    }

    static OrganizationRelationshipEntity create(
            String id,
            String actor,
            String sourceOrganizationId,
            String targetOrganizationId,
            OrganizationRelationshipType relationshipType,
            OrganizationRelationshipStatus status,
            Instant createdAt,
            Instant updatedAt) {
        OrganizationRelationshipEntity relationship = new OrganizationRelationshipEntity();
        relationship.initialize(id, actor);
        relationship.sourceOrganizationId = sourceOrganizationId;
        relationship.targetOrganizationId = targetOrganizationId;
        relationship.relationshipType = relationshipType;
        relationship.status = status;
        relationship.setCreatedAt(createdAt != null ? createdAt : Instant.now());
        relationship.setUpdatedAt(updatedAt != null ? updatedAt : relationship.getCreatedAt());
        return relationship;
    }

    @Column(name = "source_organization_id", length = 64, nullable = false)
    public String getSourceOrganizationId() {
        return sourceOrganizationId;
    }

    protected void setSourceOrganizationId(String sourceOrganizationId) {
        this.sourceOrganizationId = sourceOrganizationId;
    }

    @Column(name = "target_organization_id", length = 64, nullable = false)
    public String getTargetOrganizationId() {
        return targetOrganizationId;
    }

    protected void setTargetOrganizationId(String targetOrganizationId) {
        this.targetOrganizationId = targetOrganizationId;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 32, nullable = false)
    public OrganizationRelationshipType getRelationshipType() {
        return relationshipType;
    }

    protected void setRelationshipType(OrganizationRelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    public OrganizationRelationshipStatus getStatus() {
        return status;
    }

    protected void setStatus(OrganizationRelationshipStatus status) {
        this.status = status;
    }
}
