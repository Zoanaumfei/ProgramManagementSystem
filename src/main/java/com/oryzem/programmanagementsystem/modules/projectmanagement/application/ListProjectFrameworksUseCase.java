package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectFrameworkRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListProjectFrameworksUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectFrameworkRepository projectFrameworkRepository;

    public ListProjectFrameworksUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectFrameworkRepository projectFrameworkRepository) {
        this.authorizationService = authorizationService;
        this.projectFrameworkRepository = projectFrameworkRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectFrameworkViews.ProjectFrameworkView> execute(AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        return projectFrameworkRepository.findAllByOrderByDisplayNameAscCodeAsc().stream()
                .map(framework -> new ProjectFrameworkViews.ProjectFrameworkView(
                        framework.id(),
                        framework.code(),
                        framework.displayName(),
                        framework.description(),
                        framework.uiLayout(),
                        framework.active(),
                        framework.createdAt(),
                        framework.updatedAt()))
                .toList();
    }
}
