package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.Action;
import com.oryzem.programmanagementsystem.platform.authorization.AppModule;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.platform.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class OrganizationRelationshipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRelationshipRepository relationshipRepository;
    private final AuthorizationService authorizationService;
    private final OrganizationAccessService accessService;
    private final OrganizationDirectoryService organizationDirectoryService;

    OrganizationRelationshipService(
            OrganizationRepository organizationRepository,
            OrganizationRelationshipRepository relationshipRepository,
            AuthorizationService authorizationService,
            OrganizationAccessService accessService,
            OrganizationDirectoryService organizationDirectoryService) {
        this.organizationRepository = organizationRepository;
        this.relationshipRepository = relationshipRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.organizationDirectoryService = organizationDirectoryService;
    }

    @Transactional(readOnly = true)
    List<OrganizationRelationshipResponse> listRelationships(AuthenticatedUser actor, String organizationId) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        accessService.assertCanViewOrganization(actor, source);
        return relationshipRepository.findAllBySourceOrganizationIdAndStatus(
                        source.getId(),
                        OrganizationRelationshipStatus.ACTIVE).stream()
                .map(OrganizationRelationshipResponse::from)
                .toList();
    }

    OrganizationRelationshipResponse createRelationship(
            AuthenticatedUser actor,
            String organizationId,
            CreateOrganizationRelationshipRequest request) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        OrganizationEntity target = findManagedOrganization(request.targetOrganizationId());
        authorizeRelationshipMutation(actor, source, target);
        validateRelationship(source, target, request.relationshipType());
        String normalizedLocalOrganizationCode = normalizeLocalOrganizationCode(request.localOrganizationCode());

        OrganizationRelationshipEntity existing = relationshipRepository
                .findBySourceOrganizationIdAndTargetOrganizationIdAndRelationshipType(
                        source.getId(),
                        target.getId(),
                        request.relationshipType())
                .orElse(null);
        String excludedRelationshipId = existing != null ? existing.getId() : null;
        validateLocalOrganizationCode(source.getId(), normalizedLocalOrganizationCode, excludedRelationshipId);

        OrganizationRelationshipEntity saved = java.util.Optional.ofNullable(existing)
                .map(relationship -> reactivateRelationship(relationship, actor.username(), normalizedLocalOrganizationCode))
                .orElseGet(() -> relationshipRepository.save(OrganizationRelationshipEntity.create(
                        OrganizationIds.newId("REL"),
                        actor.username(),
                        source.getId(),
                        target.getId(),
                        request.relationshipType(),
                        normalizedLocalOrganizationCode,
                        OrganizationRelationshipStatus.ACTIVE,
                        Instant.now(),
                        Instant.now())));
        return OrganizationRelationshipResponse.from(saved);
    }

    OrganizationRelationshipResponse updateRelationship(
            AuthenticatedUser actor,
            String organizationId,
            String relationshipId,
            UpdateOrganizationRelationshipRequest request) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        OrganizationRelationshipEntity relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationRelationship", relationshipId));
        if (!source.getId().equals(relationship.getSourceOrganizationId())) {
            throw new ResourceNotFoundException("OrganizationRelationship", relationshipId);
        }
        OrganizationEntity target = findManagedOrganization(relationship.getTargetOrganizationId());
        authorizeRelationshipMutation(actor, source, target);

        String normalizedLocalOrganizationCode = normalizeLocalOrganizationCode(request.localOrganizationCode());
        validateLocalOrganizationCode(source.getId(), normalizedLocalOrganizationCode, relationship.getId());

        relationship.setLocalOrganizationCode(normalizedLocalOrganizationCode);
        relationship.touch(actor.username());
        OrganizationRelationshipEntity saved = relationshipRepository.save(relationship);
        return OrganizationRelationshipResponse.from(saved);
    }

    OrganizationRelationshipResponse inactivateRelationship(
            AuthenticatedUser actor,
            String organizationId,
            String relationshipId) {
        OrganizationEntity source = findManagedOrganization(organizationId);
        OrganizationRelationshipEntity relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationRelationship", relationshipId));
        if (!source.getId().equals(relationship.getSourceOrganizationId())) {
            throw new ResourceNotFoundException("OrganizationRelationship", relationshipId);
        }
        OrganizationEntity target = findManagedOrganization(relationship.getTargetOrganizationId());
        authorizeRelationshipMutation(actor, source, target);

        relationship.setStatus(OrganizationRelationshipStatus.INACTIVE);
        relationship.touch(actor.username());
        OrganizationRelationshipEntity saved = relationshipRepository.save(relationship);
        return OrganizationRelationshipResponse.from(saved);
    }

    private void authorizeRelationshipMutation(
            AuthenticatedUser actor,
            OrganizationEntity source,
            OrganizationEntity target) {
        AuthorizationDecision decision = authorizationService.decide(
                actor,
                AuthorizationContext.builder(AppModule.TENANT, Action.CREATE)
                        .resourceTenantId(source.getTenantId())
                        .resourceTenantType(source.getTenantType())
                        .build());
        if (!decision.allowed()) {
            throw new AccessDeniedException(decision.reason());
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.INTERNAL) {
            return;
        }

        if (actor.hasRole(Role.ADMIN) && actor.tenantType() == TenantType.EXTERNAL) {
            if (actor.organizationId() == null || !accessService.visibleOrganizationIds(actor, false).contains(source.getId())) {
                throw new AccessDeniedException("Organization is outside the manageable relationship scope for the authenticated user.");
            }
            if (!source.getTenantId().equals(actor.tenantId()) || !target.getTenantId().equals(actor.tenantId())) {
                throw new AccessDeniedException("Relationship creation is restricted to the active tenant.");
            }
            return;
        }

        throw new AccessDeniedException("Only admins can manage organization relationships.");
    }

    private void validateRelationship(
            OrganizationEntity source,
            OrganizationEntity target,
            OrganizationRelationshipType relationshipType) {
        if (source.getId().equals(target.getId())) {
            throw selfRelationshipNotAllowed(source.getId());
        }
        if (relationshipType == OrganizationRelationshipType.CUSTOMER_SUPPLIER
                && organizationDirectoryService.collectSubtreeIds(target.getId()).contains(source.getId())) {
            throw relationshipCycleNotAllowed(source.getId(), target.getId());
        }
    }

    private OrganizationRelationshipEntity reactivateRelationship(
            OrganizationRelationshipEntity relationship,
            String actor,
            String localOrganizationCode) {
        boolean changed = !java.util.Objects.equals(relationship.getLocalOrganizationCode(), localOrganizationCode);
        relationship.setLocalOrganizationCode(localOrganizationCode);
        if (relationship.getStatus() == OrganizationRelationshipStatus.ACTIVE) {
            if (changed) {
                relationship.touch(actor);
                return relationshipRepository.save(relationship);
            }
            return relationship;
        }
        relationship.setStatus(OrganizationRelationshipStatus.ACTIVE);
        relationship.touch(actor);
        return relationshipRepository.save(relationship);
    }

    private void validateLocalOrganizationCode(
            String sourceOrganizationId,
            String localOrganizationCode,
            String excludedRelationshipId) {
        if (localOrganizationCode == null) {
            return;
        }
        boolean exists = excludedRelationshipId == null
                ? relationshipRepository.existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCase(
                        sourceOrganizationId,
                        localOrganizationCode)
                : relationshipRepository.existsBySourceOrganizationIdAndLocalOrganizationCodeIgnoreCaseAndIdNot(
                        sourceOrganizationId,
                        localOrganizationCode,
                        excludedRelationshipId);
        if (exists) {
            throw localOrganizationCodeAlreadyExists(sourceOrganizationId, localOrganizationCode);
        }
    }

    private String normalizeLocalOrganizationCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(java.util.Locale.ROOT);
    }

    private OrganizationEntity findManagedOrganization(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        if (organization.getTenantType() != TenantType.EXTERNAL) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }
        return organization;
    }

    private BusinessRuleException selfRelationshipNotAllowed(String sourceOrganizationId) {
        return new BusinessRuleException(
                "ORGANIZATION_SELF_RELATIONSHIP_NOT_ALLOWED",
                "An organization cannot create a relationship with itself.",
                Map.of("sourceOrganizationId", sourceOrganizationId));
    }

    private BusinessRuleException relationshipCycleNotAllowed(String sourceOrganizationId, String targetOrganizationId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sourceOrganizationId", sourceOrganizationId);
        details.put("targetOrganizationId", targetOrganizationId);
        return new BusinessRuleException(
                "ORGANIZATION_RELATIONSHIP_CYCLE_NOT_ALLOWED",
                "Relationship would create a cycle in the customer/supplier graph.",
                details);
    }

    private BusinessRuleException localOrganizationCodeAlreadyExists(
            String sourceOrganizationId,
            String localOrganizationCode) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", "localOrganizationCode");
        details.put("value", localOrganizationCode);
        details.put("sourceOrganizationId", sourceOrganizationId);
        return new BusinessRuleException(
                "ORGANIZATION_RELATIONSHIP_LOCAL_CODE_ALREADY_EXISTS",
                "Local organization code already exists for this organization.",
                details);
    }
}
