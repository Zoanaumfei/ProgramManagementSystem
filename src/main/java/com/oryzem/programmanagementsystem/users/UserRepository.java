package com.oryzem.programmanagementsystem.users;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<ManagedUser> findAll();

    List<ManagedUser> findByTenantId(String tenantId);

    Optional<ManagedUser> findById(String userId);

    Optional<ManagedUser> findByEmailIgnoreCase(String email);

    ManagedUser save(ManagedUser user);

    void deleteById(String userId);

    void deleteAll();
}
