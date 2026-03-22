package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Transactional
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserJpaRepository delegate;

    public JpaUserRepository(SpringDataUserJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedUser> findAll() {
        return delegate.findAll().stream()
                .map(UserEntity::toDomain)
                .sorted(java.util.Comparator.comparing(ManagedUser::createdAt).thenComparing(ManagedUser::id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedUser> findByTenantId(String tenantId) {
        return delegate.findByTenantIdOrderByCreatedAtAscIdAsc(tenantId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findById(String userId) {
        return delegate.findById(userId).map(UserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByIdentityUsername(String identityUsername) {
        return delegate.findByIdentityUsernameIgnoreCase(identityUsername).map(UserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByIdentitySubject(String identitySubject) {
        return delegate.findByIdentitySubject(identitySubject).map(UserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByEmailIgnoreCase(String email) {
        return delegate.findByEmailIgnoreCase(email).map(UserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInvitedOrActiveAdmin(String tenantId) {
        return delegate.existsByTenantIdAndRoleAndStatusIn(
                tenantId,
                Role.ADMIN,
                List.of(UserStatus.INVITED, UserStatus.ACTIVE));
    }

    @Override
    public ManagedUser save(ManagedUser user) {
        return delegate.save(UserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public void deleteById(String userId) {
        delegate.deleteById(userId);
    }

    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}

