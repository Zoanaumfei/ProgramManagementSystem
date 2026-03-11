package com.oryzem.programmanagementsystem.web;

import com.oryzem.programmanagementsystem.authorization.Action;
import com.oryzem.programmanagementsystem.authorization.AppModule;
import com.oryzem.programmanagementsystem.authorization.AuthorizationContext;
import com.oryzem.programmanagementsystem.authorization.AuthorizationDecision;
import com.oryzem.programmanagementsystem.authorization.AuthorizationService;
import com.oryzem.programmanagementsystem.authorization.Role;
import com.oryzem.programmanagementsystem.authorization.TenantType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authz")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/check")
    public Map<String, Object> check(
            Authentication authentication,
            @RequestParam AppModule module,
            @RequestParam Action action,
            @RequestParam(required = false) String resourceTenantId,
            @RequestParam(required = false) TenantType resourceTenantType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String resourceOwnerUserId,
            @RequestParam(required = false) String resourceStatus,
            @RequestParam(required = false) Role targetRole,
            @RequestParam(required = false) String targetUserId,
            @RequestParam(defaultValue = "false") boolean auditTrailEnabled,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(defaultValue = "false") boolean sensitiveDataRequested,
            @RequestParam(defaultValue = "false") boolean maskedViewRequested,
            @RequestParam(required = false) String justification) {

        AuthorizationContext context = AuthorizationContext.builder(module, action)
                .resourceTenantId(resourceTenantId)
                .resourceTenantType(resourceTenantType)
                .resourceId(resourceId)
                .resourceOwnerUserId(resourceOwnerUserId)
                .resourceStatus(resourceStatus)
                .targetRole(targetRole)
                .targetUserId(targetUserId)
                .auditTrailEnabled(auditTrailEnabled)
                .supportOverride(supportOverride)
                .sensitiveDataRequested(sensitiveDataRequested)
                .maskedViewRequested(maskedViewRequested)
                .justification(justification)
                .build();

        AuthorizationDecision decision = authorizationService.decide(authentication, context);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowed", decision.allowed());
        body.put("reason", decision.reason());
        body.put("module", module.name());
        body.put("action", action.name());
        body.put("restrictions", decision.restrictions().stream().map(Enum::name).sorted().toList());
        body.put("auditRequired", decision.auditRequired());
        body.put("maskedViewRequired", decision.maskedViewRequired());
        body.put("crossTenant", decision.crossTenant());
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
