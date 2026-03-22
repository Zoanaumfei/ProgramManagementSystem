package com.oryzem.programmanagementsystem.platform.users.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<ManagedUser> findAll();

    List<ManagedUser> findByTenantId(String tenantId);

    Optional<ManagedUser> findById(String userId);

    Optional<ManagedUser> findByIdentityUsername(String identityUsername);

    Optional<ManagedUser> findByIdentitySubject(String identitySubject);

    Optional<ManagedUser> findByEmailIgnoreCase(String email);

    boolean hasInvitedOrActiveAdmin(String tenantId);

    ManagedUser save(ManagedUser user);

    void deleteById(String userId);

    void deleteAll();
}
