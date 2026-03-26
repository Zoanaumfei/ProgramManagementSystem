package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.auth.AuthenticatedIdentityContext;
import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationGateway;
import com.oryzem.programmanagementsystem.platform.auth.CurrentUserEmailVerificationState;
import com.oryzem.programmanagementsystem.platform.auth.EmailVerificationCodeDelivery;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class StubUserIdentityGateway implements UserIdentityGateway, CurrentUserEmailVerificationGateway {

    private static final String STUB_VERIFICATION_CODE = "123456";

    private final List<StubUserIdentityOperation> operations = new ArrayList<>();
    private final Set<String> existingIdentityUsernames = new HashSet<>();
    private final Set<Role> bootstrapRoles = new LinkedHashSet<>();
    private final Map<String, Boolean> emailVerificationByIdentityKey = new HashMap<>();

    @Override
    public void createUser(ManagedUser user) {
        operations.add(operation("CREATE", user));
        trackIdentity(user);
    }

    @Override
    public void updateUser(ManagedUser existingUser, ManagedUser updatedUser) {
        operations.add(operation("UPDATE", updatedUser));
    }

    @Override
    public void resendInvite(ManagedUser user) {
        operations.add(operation("RESEND_INVITE", user));
        trackIdentity(user);
    }

    @Override
    public void resetAccess(ManagedUser user) {
        if (!isRecoveryChannelVerified(identityKey(user.identityUsername(), user.email(), user.id()))) {
            throw new ConflictException("The user must verify email before access reset can be used.");
        }
        operations.add(operation("RESET_ACCESS", user));
    }

    @Override
    public void disableUser(ManagedUser user) {
        operations.add(operation("DISABLE", user));
    }

    @Override
    public void deleteUser(ManagedUser user) {
        operations.add(operation("DELETE", user));
        markIdentityMissing(user.identityUsername());
    }

    @Override
    public boolean identityExists(ManagedUser user) {
        return user.identityUsername() != null
                && existingIdentityUsernames.contains(user.identityUsername().toLowerCase(Locale.ROOT));
    }

    @Override
    public void ensureBootstrapUser(ManagedUser user, Set<Role> grantedRoles, String password, String temporaryPassword) {
        operations.add(operation("BOOTSTRAP_ENSURE", user));
        trackIdentity(user);
        trackBootstrapGroups(grantedRoles);
    }

    public List<StubUserIdentityOperation> operations() {
        return List.copyOf(operations);
    }

    public void markIdentityPresent(String identityUsername) {
        if (identityUsername != null && !identityUsername.isBlank()) {
            existingIdentityUsernames.add(identityUsername.toLowerCase(Locale.ROOT));
        }
    }

    public void markIdentityMissing(String identityUsername) {
        if (identityUsername != null && !identityUsername.isBlank()) {
            existingIdentityUsernames.remove(identityUsername.toLowerCase(Locale.ROOT));
        }
    }

    public void clear() {
        operations.clear();
        existingIdentityUsernames.clear();
        bootstrapRoles.clear();
        emailVerificationByIdentityKey.clear();
    }

    public Set<Role> bootstrapRoles() {
        return Set.copyOf(bootstrapRoles);
    }

    @Override
    public CurrentUserEmailVerificationState describeCurrentUser(AuthenticatedIdentityContext context) {
        String identityKey = identityKey(context.username(), context.email(), context.subject());
        boolean verified = emailVerificationByIdentityKey.computeIfAbsent(
                identityKey,
                ignored -> context.emailVerifiedClaim() != null ? context.emailVerifiedClaim() : true);
        return new CurrentUserEmailVerificationState(context.email(), verified);
    }

    @Override
    public EmailVerificationCodeDelivery sendCurrentUserEmailVerificationCode(AuthenticatedIdentityContext context) {
        if (!StringUtils.hasText(context.email())) {
            throw new IllegalArgumentException("The authenticated user does not expose an email for verification.");
        }

        String identityKey = identityKey(context.username(), context.email(), context.subject());
        emailVerificationByIdentityKey.putIfAbsent(identityKey, false);
        return new EmailVerificationCodeDelivery("email", "EMAIL", maskEmail(context.email()));
    }

    @Override
    public CurrentUserEmailVerificationState verifyCurrentUserEmail(AuthenticatedIdentityContext context, String code) {
        if (!STUB_VERIFICATION_CODE.equals(code)) {
            throw new IllegalArgumentException("The email verification code is invalid.");
        }

        String identityKey = identityKey(context.username(), context.email(), context.subject());
        emailVerificationByIdentityKey.put(identityKey, true);
        return new CurrentUserEmailVerificationState(context.email(), true);
    }

    public void markRecoveryChannelVerified(String identityUsername) {
        emailVerificationByIdentityKey.put(identityKey(identityUsername, identityUsername, identityUsername), true);
    }

    public void markRecoveryChannelUnverified(String identityUsername) {
        emailVerificationByIdentityKey.put(identityKey(identityUsername, identityUsername, identityUsername), false);
    }

    private StubUserIdentityOperation operation(String action, ManagedUser user) {
        return new StubUserIdentityOperation(
                action,
                user.identityUsername(),
                user.email());
    }

    private void trackIdentity(ManagedUser user) {
        markIdentityPresent(user.identityUsername());
    }

    private boolean isRecoveryChannelVerified(String identityKey) {
        return emailVerificationByIdentityKey.getOrDefault(identityKey, true);
    }

    private void trackBootstrapGroups(Set<Role> grantedRoles) {
        if (grantedRoles == null || grantedRoles.isEmpty()) {
            bootstrapRoles.add(Role.ADMIN);
            return;
        }
        bootstrapRoles.addAll(grantedRoles);
    }

    private String identityKey(String username, String email, String fallback) {
        if (StringUtils.hasText(username)) {
            return username.toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(email)) {
            return email.toLowerCase(Locale.ROOT);
        }
        return fallback == null ? "unknown" : fallback.toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex - 1);
    }
}
