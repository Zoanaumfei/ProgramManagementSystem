package com.oryzem.programmanagementsystem.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findByTenantIdOrderByCreatedAtAscIdAsc(String tenantId);

    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
