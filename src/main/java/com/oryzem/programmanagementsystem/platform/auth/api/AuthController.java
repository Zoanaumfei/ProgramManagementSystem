package com.oryzem.programmanagementsystem.platform.auth.api;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.platform.access.ActiveAccessContextDescriptorService;
import com.oryzem.programmanagementsystem.platform.auth.AuthEmailVerificationService;
import com.oryzem.programmanagementsystem.platform.auth.CognitoProperties;
import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationState;
import com.oryzem.programmanagementsystem.platform.auth.PublicAuthenticationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String USERNAME_CLAIM = "cognito:username";
    private static final String ACCESS_TOKEN_USERNAME_CLAIM = "username";

    private final CognitoProperties cognitoProperties;
    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final ActiveAccessContextDescriptorService activeAccessContextDescriptorService;
    private final AuthEmailVerificationService authEmailVerificationService;
    private final PublicAuthenticationService publicAuthenticationService;

    public AuthController(
            CognitoProperties cognitoProperties,
            AuthenticatedUserMapper authenticatedUserMapper,
            ActiveAccessContextDescriptorService activeAccessContextDescriptorService,
            AuthEmailVerificationService authEmailVerificationService,
            PublicAuthenticationService publicAuthenticationService) {
        this.cognitoProperties = cognitoProperties;
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.activeAccessContextDescriptorService = activeAccessContextDescriptorService;
        this.authEmailVerificationService = authEmailVerificationService;
        this.publicAuthenticationService = publicAuthenticationService;
    }

    @GetMapping("/public/auth/config")
    public Map<String, Object> authConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", "aws-cognito");
        body.put("mode", "custom-login-ready");
        body.put("issuerUri", cognitoProperties.issuerUri());
        body.put("jwkSetUri", cognitoProperties.jwkSetUri());
        body.put("appClientId", cognitoProperties.appClientId());
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @PostMapping("/public/auth/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthenticationResponse.from(publicAuthenticationService.authenticate(request.username(), request.password()));
    }

    @PostMapping("/public/auth/login/new-password")
    public AuthenticationResponse completeNewPasswordChallenge(
            @Valid @RequestBody CompleteNewPasswordChallengeRequest request) {
        return AuthenticationResponse.from(publicAuthenticationService.completeNewPasswordChallenge(
                request.username(),
                request.session(),
                request.newPassword()));
    }

    @PostMapping("/public/auth/password-reset/code")
    public PasswordResetResponse sendPasswordResetCode(@Valid @RequestBody StartPasswordResetRequest request) {
        return PasswordResetResponse.codeSent(publicAuthenticationService.sendPasswordResetCode(request.username()));
    }

    @PostMapping("/public/auth/password-reset/confirm")
    public PasswordResetResponse confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        publicAuthenticationService.confirmPasswordReset(request.username(), request.code(), request.newPassword());
        return PasswordResetResponse.confirmed(request.username());
    }

    @PostMapping("/public/auth/refresh")
    public AuthenticationResponse refresh(@Valid @RequestBody RefreshSessionRequest request) {
        return AuthenticationResponse.authenticated(
                request.username(),
                publicAuthenticationService.refreshSession(request.username(), request.refreshToken()));
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> me(Authentication authentication) {
        JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthentication.getToken();
        AuthenticatedUser authenticatedUser = authenticatedUserMapper.from(authentication);
        ActiveAccessContextDescriptorService.ActiveAccessContextLabels activeContextLabels =
                activeAccessContextDescriptorService.describe(
                        authenticatedUser.activeTenantId(),
                        authenticatedUser.activeOrganizationId(),
                        authenticatedUser.activeMarketId());
        CurrentUserEmailVerificationState emailVerificationState = authEmailVerificationService.describe(authentication);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", jwt.getSubject());
        body.put("username", firstNonBlank(
                jwt.getClaimAsString(USERNAME_CLAIM),
                jwt.getClaimAsString(ACCESS_TOKEN_USERNAME_CLAIM),
                jwtAuthentication.getName()));
        body.put("email", nullableFirstNonBlank(emailVerificationState.email(), jwt.getClaimAsString("email")));
        body.put("emailVerified", emailVerificationState.emailVerified());
        body.put("emailVerificationRequired", emailVerificationState.emailVerificationRequired());
        body.put("tokenUse", jwt.getClaimAsString("token_use"));
        body.put("userId", authenticatedUser.userId());
        body.put("membershipId", authenticatedUser.membershipId());
        body.put("activeTenantId", authenticatedUser.activeTenantId());
        body.put("activeTenantName", activeContextLabels.tenantName());
        body.put("activeOrganizationId", authenticatedUser.activeOrganizationId());
        body.put("activeOrganizationName", activeContextLabels.organizationName());
        body.put("activeMarketId", authenticatedUser.activeMarketId());
        body.put("activeMarketName", activeContextLabels.marketName());
        body.put("tenantType", authenticatedUser.tenantType() != null ? authenticatedUser.tenantType().name() : null);
        body.put("roles", authenticatedUser.roles().stream().map(Enum::name).sorted().toList());
        body.put("permissions", authenticatedUser.permissions().stream().sorted().toList());
        body.put("groups", defaultList(jwt.getClaimAsStringList(GROUPS_CLAIM)));
        body.put("scopes", scopes(jwt.getClaimAsString("scope")));
        body.put("authorities", jwtAuthentication.getAuthorities().stream()
                .map(Object::toString)
                .sorted()
                .toList());
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/api/auth/email-verification")
    public EmailVerificationResponse emailVerification(Authentication authentication) {
        return EmailVerificationResponse.snapshot(authEmailVerificationService.describe(authentication));
    }

    @PostMapping("/api/auth/email-verification/code")
    public EmailVerificationResponse sendEmailVerificationCode(Authentication authentication) {
        CurrentUserEmailVerificationState state = authEmailVerificationService.describe(authentication);
        var delivery = authEmailVerificationService.sendCode(authentication);
        return EmailVerificationResponse.codeSent(
                state,
                delivery.attributeName(),
                delivery.deliveryMedium(),
                delivery.destination());
    }

    @PostMapping("/api/auth/email-verification/confirm")
    public EmailVerificationResponse confirmEmailVerification(
            Authentication authentication,
            @Valid @RequestBody ConfirmEmailVerificationRequest request) {
        return EmailVerificationResponse.verified(authEmailVerificationService.confirmCode(authentication, request.code()));
    }

    @PostMapping("/api/auth/logout")
    public LogoutResponse logout(Authentication authentication) {
        publicAuthenticationService.signOut(authentication);
        return LogoutResponse.signedOut();
    }

    private String firstNonBlank(String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("unknown");
    }

    private String nullableFirstNonBlank(String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> scopes(String scopeClaim) {
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return List.of();
        }

        return Stream.of(scopeClaim.trim().split("\\s+"))
                .filter(scope -> !scope.isBlank())
                .toList();
    }
}

