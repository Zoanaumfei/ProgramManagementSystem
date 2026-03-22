package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findByTenantIdOrderByCreatedAtAscIdAsc(String tenantId);

    Optional<UserEntity> findByIdentityUsernameIgnoreCase(String identityUsername);

    Optional<UserEntity> findByIdentitySubject(String identitySubject);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByTenantIdAndRoleAndStatusIn(String tenantId, Role role, Collection<UserStatus> statuses);
}

