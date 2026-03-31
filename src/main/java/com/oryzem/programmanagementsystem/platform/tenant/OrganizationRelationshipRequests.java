package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

record CreateOrganizationRelationshipRequest(
        @NotBlank String targetOrganizationId,
        @NotNull OrganizationRelationshipType relationshipType,
        String localOrganizationCode) {
}

record UpdateOrganizationRelationshipRequest(
        String localOrganizationCode) {
}

record OrganizationRelationshipResponse(
        String id,
        String sourceOrganizationId,
        String targetOrganizationId,
        OrganizationRelationshipType relationshipType,
        String localOrganizationCode,
        OrganizationRelationshipStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static OrganizationRelationshipResponse from(OrganizationRelationshipEntity relationship) {
        return new OrganizationRelationshipResponse(
                relationship.getId(),
                relationship.getSourceOrganizationId(),
                relationship.getTargetOrganizationId(),
                relationship.getRelationshipType(),
                relationship.getLocalOrganizationCode(),
                relationship.getStatus(),
                relationship.getCreatedAt(),
                relationship.getUpdatedAt());
    }
}
