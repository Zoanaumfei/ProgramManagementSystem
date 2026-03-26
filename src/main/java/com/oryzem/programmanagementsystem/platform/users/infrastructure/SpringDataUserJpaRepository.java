package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByIdentityUsernameIgnoreCase(String identityUsername);

    Optional<UserEntity> findByIdentitySubject(String identitySubject);

    Optional<UserEntity> findByEmailIgnoreCase(String email);
}

