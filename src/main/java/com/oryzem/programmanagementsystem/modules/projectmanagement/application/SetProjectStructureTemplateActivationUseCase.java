package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetProjectStructureTemplateActivationUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectViewMapper viewMapper;

    public SetProjectStructureTemplateActivationUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureTemplateSummaryView execute(String structureTemplateId, boolean active, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        ProjectStructureTemplateAggregate entity = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        administrationService.authorizeManagement(actor, entity.ownerOrganizationId());
        ProjectStructureTemplateAggregate updated = structureTemplateRepository.save(entity.withActive(active));
        return viewMapper.toStructureTemplateSummaryView(updated);
    }
}


