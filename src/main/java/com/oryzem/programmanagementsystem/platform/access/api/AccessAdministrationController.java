package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.AccessAdministrationService;
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
@RequestMapping("/api/access")
public class AccessAdministrationController {

    private final AccessAdministrationService accessAdministrationService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public AccessAdministrationController(
            AccessAdministrationService accessAdministrationService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.accessAdministrationService = accessAdministrationService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping("/users/{userId}/memberships")
    public List<MembershipResponse> listMemberships(Authentication authentication, @PathVariable String userId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.listMemberships(actor, userId);
    }

    @PostMapping("/users/{userId}/memberships")
    public ResponseEntity<MembershipResponse> createMembership(
            Authentication authentication,
            @PathVariable String userId,
            @Valid @RequestBody CreateMembershipRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        MembershipResponse created = accessAdministrationService.createMembership(actor, userId, request);
        return ResponseEntity.created(URI.create("/api/access/users/" + userId + "/memberships/" + created.id()))
                .body(created);
    }

    @PutMapping("/users/{userId}/memberships/{membershipId}")
    public MembershipResponse updateMembership(
            Authentication authentication,
            @PathVariable String userId,
            @PathVariable String membershipId,
            @Valid @RequestBody UpdateMembershipRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.updateMembership(actor, userId, membershipId, request);
    }

    @DeleteMapping("/users/{userId}/memberships/{membershipId}")
    public MembershipResponse inactivateMembership(
            Authentication authentication,
            @PathVariable String userId,
            @PathVariable String membershipId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.inactivateMembership(actor, userId, membershipId);
    }

    @PostMapping("/context/activate")
    public ActiveAccessContextResponse activateMembership(
            Authentication authentication,
            @Valid @RequestBody ActivateMembershipRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.activateMembership(actor, request);
    }

    @GetMapping("/tenants")
    public List<TenantSummaryResponse> listVisibleTenants(Authentication authentication) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.listVisibleTenants(actor);
    }

    @GetMapping("/tenants/{tenantId}/markets")
    public List<TenantMarketResponse> listTenantMarkets(
            Authentication authentication,
            @PathVariable String tenantId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.listTenantMarkets(actor, tenantId);
    }

    @PostMapping("/tenants/{tenantId}/markets")
    public ResponseEntity<TenantMarketResponse> createTenantMarket(
            Authentication authentication,
            @PathVariable String tenantId,
            @Valid @RequestBody CreateTenantMarketRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        TenantMarketResponse created = accessAdministrationService.createTenantMarket(actor, tenantId, request);
        return ResponseEntity.created(URI.create("/api/access/tenants/" + tenantId + "/markets/" + created.id()))
                .body(created);
    }

    @PutMapping("/tenants/{tenantId}/markets/{marketId}")
    public TenantMarketResponse updateTenantMarket(
            Authentication authentication,
            @PathVariable String tenantId,
            @PathVariable String marketId,
            @Valid @RequestBody UpdateTenantMarketRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.updateTenantMarket(actor, tenantId, marketId, request);
    }

    @DeleteMapping("/tenants/{tenantId}/markets/{marketId}")
    public TenantMarketResponse inactivateTenantMarket(
            Authentication authentication,
            @PathVariable String tenantId,
            @PathVariable String marketId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return accessAdministrationService.inactivateTenantMarket(actor, tenantId, marketId);
    }
}
