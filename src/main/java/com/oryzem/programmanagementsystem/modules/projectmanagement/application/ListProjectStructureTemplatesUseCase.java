package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.read.StructureReadModels;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectStructureTemplatesUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public ListProjectStructureTemplatesUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.viewMapper = viewMapper;
    }

    public List<StructureReadModels.ProjectStructureTemplateListReadModel> execute(AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        if (actor == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated user is required.");
        }
        return structureTemplateRepository.findAllByOrderByFrameworkTypeAscVersionDescNameAsc().stream()
                .map(viewMapper::toProjectStructureTemplateListReadModel)
                .toList();
    }
}



