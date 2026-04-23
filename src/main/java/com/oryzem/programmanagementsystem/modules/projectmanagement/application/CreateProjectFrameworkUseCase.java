package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectFrameworkRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkUiLayout;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectFrameworkUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectFrameworkRepository projectFrameworkRepository;

    public CreateProjectFrameworkUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectFrameworkRepository projectFrameworkRepository) {
        this.authorizationService = authorizationService;
        this.projectFrameworkRepository = projectFrameworkRepository;
    }

    @Transactional
    public ProjectFrameworkViews.ProjectFrameworkView execute(CreateProjectFrameworkCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        assertInternalAdmin(actor);
        String normalizedCode = normalizeCode(command.code());
        if (projectFrameworkRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_CODE_ALREADY_EXISTS", "A project framework with the same code already exists.");
        }
        Instant now = Instant.now();
        ProjectFrameworkAggregate framework = projectFrameworkRepository.save(new ProjectFrameworkAggregate(
                ProjectIds.newProjectFrameworkId(),
                normalizedCode,
                command.displayName().trim(),
                normalizeDescription(command.description()),
                command.uiLayout(),
                command.active(),
                now,
                now));
        return new ProjectFrameworkViews.ProjectFrameworkView(
                framework.id(),
                framework.code(),
                framework.displayName(),
                framework.description(),
                framework.uiLayout(),
                framework.active(),
                framework.createdAt(),
                framework.updatedAt());
    }

    private void assertInternalAdmin(AuthenticatedUser actor) {
        if (actor == null || actor.tenantType() != TenantType.INTERNAL || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only internal admins can manage project frameworks.");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("PROJECT_FRAMEWORK_CODE_REQUIRED", "Project framework code is required.");
        }
        return code.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    public record CreateProjectFrameworkCommand(
            String code,
            String displayName,
            String description,
            ProjectFrameworkUiLayout uiLayout,
            boolean active) {
    }
}
