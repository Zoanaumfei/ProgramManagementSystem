package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserIdentityGateway;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StubUserIdentityGateway implements UserIdentityGateway {

    private final List<StubUserIdentityOperation> operations = new ArrayList<>();
    private final Set<String> existingIdentityUsernames = new HashSet<>();
    private final Set<Role> bootstrapRoles = new LinkedHashSet<>();

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
    }

    public Set<Role> bootstrapRoles() {
        return Set.copyOf(bootstrapRoles);
    }

    private StubUserIdentityOperation operation(String action, ManagedUser user) {
        return new StubUserIdentityOperation(
                action,
                user.identityUsername(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.tenantType());
    }

    private void trackIdentity(ManagedUser user) {
        markIdentityPresent(user.identityUsername());
    }

    private void trackBootstrapGroups(Set<Role> grantedRoles) {
        if (grantedRoles == null || grantedRoles.isEmpty()) {
            bootstrapRoles.add(Role.ADMIN);
            return;
        }
        bootstrapRoles.addAll(grantedRoles);
    }
}
