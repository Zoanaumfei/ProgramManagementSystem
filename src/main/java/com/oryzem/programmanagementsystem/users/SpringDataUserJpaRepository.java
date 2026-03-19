package com.oryzem.programmanagementsystem.users;

import com.oryzem.programmanagementsystem.authorization.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findByTenantIdOrderByCreatedAtAscIdAsc(String tenantId);

    Optional<UserEntity> findByIdentityUsernameIgnoreCase(String identityUsername);

    Optional<UserEntity> findByIdentitySubject(String identitySubject);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByTenantIdAndRoleAndStatusIn(String tenantId, Role role, Collection<UserStatus> statuses);
}
