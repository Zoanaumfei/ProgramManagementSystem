package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectFrameworkRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectFrameworkUiLayout;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProjectFrameworkUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectFrameworkRepository projectFrameworkRepository;

    public UpdateProjectFrameworkUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectFrameworkRepository projectFrameworkRepository) {
        this.authorizationService = authorizationService;
        this.projectFrameworkRepository = projectFrameworkRepository;
    }

    @Transactional
    public ProjectFrameworkViews.ProjectFrameworkView execute(String frameworkId, UpdateProjectFrameworkCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        assertInternalAdmin(actor);
        ProjectFrameworkAggregate current = projectFrameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFramework", frameworkId));
        ProjectFrameworkAggregate updated = projectFrameworkRepository.save(
                current.update(command.displayName(), command.description(), command.uiLayout(), command.active(), Instant.now()));
        return new ProjectFrameworkViews.ProjectFrameworkView(
                updated.id(),
                updated.code(),
                updated.displayName(),
                updated.description(),
                updated.uiLayout(),
                updated.active(),
                updated.createdAt(),
                updated.updatedAt());
    }

    private void assertInternalAdmin(AuthenticatedUser actor) {
        if (actor == null || actor.tenantType() != TenantType.INTERNAL || !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Only internal admins can manage project frameworks.");
        }
    }

    public record UpdateProjectFrameworkCommand(
            String displayName,
            String description,
            ProjectFrameworkUiLayout uiLayout,
            boolean active) {
    }
}
