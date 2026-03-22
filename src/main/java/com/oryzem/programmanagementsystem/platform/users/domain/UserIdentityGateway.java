package com.oryzem.programmanagementsystem.platform.users.domain;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import java.util.Set;

public interface UserIdentityGateway {

    void createUser(ManagedUser user);

    void updateUser(ManagedUser existingUser, ManagedUser updatedUser);

    void resendInvite(ManagedUser user);

    void resetAccess(ManagedUser user);

    void disableUser(ManagedUser user);

    void deleteUser(ManagedUser user);

    boolean identityExists(ManagedUser user);

    void ensureBootstrapUser(ManagedUser user, Set<Role> grantedRoles, String password, String temporaryPassword);
}

