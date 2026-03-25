package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
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
    private final AccessContextService accessContextService;

    public JpaUserRepository(SpringDataUserJpaRepository delegate, AccessContextService accessContextService) {
        this.delegate = delegate;
        this.accessContextService = accessContextService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedUser> findAll() {
        return delegate.findAll().stream()
                .map(UserEntity::toDomain)
                .map(accessContextService::hydrateLegacyCompatibilityView)
                .sorted(java.util.Comparator.comparing(ManagedUser::createdAt).thenComparing(ManagedUser::id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedUser> findByTenantId(String tenantId) {
        return findAll().stream()
                .filter(user -> tenantId != null && tenantId.equals(user.tenantId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findById(String userId) {
        return delegate.findById(userId)
                .map(UserEntity::toDomain)
                .map(accessContextService::hydrateLegacyCompatibilityView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByIdentityUsername(String identityUsername) {
        return delegate.findByIdentityUsernameIgnoreCase(identityUsername)
                .map(UserEntity::toDomain)
                .map(accessContextService::hydrateLegacyCompatibilityView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByIdentitySubject(String identitySubject) {
        return delegate.findByIdentitySubject(identitySubject)
                .map(UserEntity::toDomain)
                .map(accessContextService::hydrateLegacyCompatibilityView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagedUser> findByEmailIgnoreCase(String email) {
        return delegate.findByEmailIgnoreCase(email)
                .map(UserEntity::toDomain)
                .map(accessContextService::hydrateLegacyCompatibilityView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInvitedOrActiveAdmin(String tenantId) {
        return findByTenantId(tenantId).stream()
                .anyMatch(user -> user.role() == Role.ADMIN
                        && List.of(UserStatus.INVITED, UserStatus.ACTIVE).contains(user.status()));
    }

    @Override
    public ManagedUser save(ManagedUser user) {
        ManagedUser saved = delegate.save(UserEntity.fromDomain(user)).toDomain();
        accessContextService.synchronizeDefaultMembership(saved);
        return saved;
    }

    @Override
    public void deleteById(String userId) {
        accessContextService.deleteMembershipsForUser(userId);
        delegate.deleteById(userId);
    }

    @Override
    public void deleteAll() {
        delegate.findAll().stream()
                .map(UserEntity::toDomain)
                .map(ManagedUser::id)
                .forEach(accessContextService::deleteMembershipsForUser);
        delegate.deleteAll();
    }
}

