package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectFrameworkRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaProjectFrameworkRepositoryAdapter implements ProjectFrameworkRepository {

    private final SpringDataProjectFrameworkJpaRepository delegate;

    public JpaProjectFrameworkRepositoryAdapter(SpringDataProjectFrameworkJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<ProjectFrameworkAggregate> findById(String id) {
        return delegate.findById(id).map(ProjectFrameworkEntity::toDomain);
    }

    @Override
    public Optional<ProjectFrameworkAggregate> findByCode(String code) {
        return delegate.findByCode(code).map(ProjectFrameworkEntity::toDomain);
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        return delegate.existsByCodeIgnoreCase(code);
    }

    @Override
    public List<ProjectFrameworkAggregate> findAllByOrderByDisplayNameAscCodeAsc() {
        return delegate.findAllByOrderByDisplayNameAscCodeAsc().stream()
                .map(ProjectFrameworkEntity::toDomain)
                .toList();
    }

    @Override
    public ProjectFrameworkAggregate save(ProjectFrameworkAggregate framework) {
        return delegate.save(ProjectFrameworkEntity.from(framework)).toDomain();
    }
}
