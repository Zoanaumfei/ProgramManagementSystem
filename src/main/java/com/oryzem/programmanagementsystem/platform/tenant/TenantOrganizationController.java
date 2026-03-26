package com.oryzem.programmanagementsystem.platform.tenant;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/organizations")
public class TenantOrganizationController {

    private final OrganizationManagementService organizationManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public TenantOrganizationController(
            OrganizationManagementService organizationManagementService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.organizationManagementService = organizationManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping
    public List<OrganizationResponse> listOrganizations(
            Authentication authentication,
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) OrganizationSetupStatus setupStatus,
            @RequestParam(required = false) String customerOrganizationId,
            @RequestParam(required = false) String parentOrganizationId,
            @RequestParam(required = false) Integer hierarchyLevel,
            @RequestParam(required = false) String search) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return organizationManagementService.listOrganizations(
                actor,
                status,
                setupStatus,
                customerOrganizationId,
                parentOrganizationId,
                hierarchyLevel,
                search);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse getOrganization(
            Authentication authentication,
            @PathVariable String organizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return organizationManagementService.getOrganization(organizationId, actor);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(organizationManagementService.createOrganization(request, actor));
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            Authentication authentication,
            @PathVariable String organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(organizationManagementService.updateOrganization(organizationId, request, actor));
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> inactivateOrganization(
            Authentication authentication,
            @PathVariable String organizationId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(organizationManagementService.inactivateOrganization(organizationId, actor));
    }

    @PostMapping("/{organizationId}/purge-subtree")
    public ResponseEntity<OrganizationPurgeResponse> purgeOrganizationSubtree(
            Authentication authentication,
            @PathVariable String organizationId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(organizationManagementService.purgeOrganizationSubtree(
                organizationId,
                actor,
                supportOverride,
                justification));
    }
}
