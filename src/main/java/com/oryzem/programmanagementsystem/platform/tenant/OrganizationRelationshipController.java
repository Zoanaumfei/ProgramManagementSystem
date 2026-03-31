package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/organizations")
public class OrganizationRelationshipController {

    private final OrganizationRelationshipService relationshipService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public OrganizationRelationshipController(
            OrganizationRelationshipService relationshipService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.relationshipService = relationshipService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping("/{organizationId}/relationships")
    public List<OrganizationRelationshipResponse> listRelationships(
            Authentication authentication,
            @PathVariable String organizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return relationshipService.listRelationships(actor, organizationId);
    }

    @PostMapping("/{organizationId}/relationships")
    public ResponseEntity<OrganizationRelationshipResponse> createRelationship(
            Authentication authentication,
            @PathVariable String organizationId,
            @Valid @RequestBody CreateOrganizationRelationshipRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        OrganizationRelationshipResponse created = relationshipService.createRelationship(actor, organizationId, request);
        return ResponseEntity.created(URI.create("/api/access/organizations/" + organizationId + "/relationships/" + created.id()))
                .body(created);
    }

    @PutMapping("/{organizationId}/relationships/{relationshipId}")
    public ResponseEntity<OrganizationRelationshipResponse> updateRelationship(
            Authentication authentication,
            @PathVariable String organizationId,
            @PathVariable String relationshipId,
            @Valid @RequestBody UpdateOrganizationRelationshipRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        OrganizationRelationshipResponse updated =
                relationshipService.updateRelationship(actor, organizationId, relationshipId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{organizationId}/relationships/{relationshipId}")
    public ResponseEntity<OrganizationRelationshipResponse> inactivateRelationship(
            Authentication authentication,
            @PathVariable String organizationId,
            @PathVariable String relationshipId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        OrganizationRelationshipResponse updated = relationshipService.inactivateRelationship(actor, organizationId, relationshipId);
        return ResponseEntity.ok(updated);
    }
}
