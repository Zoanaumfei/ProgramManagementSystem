package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurgeProjectStructureTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectStructureTemplateRepository structureTemplateRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectAuditService auditService;
    private final ProjectViewMapper viewMapper;

    public PurgeProjectStructureTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectStructureTemplateRepository structureTemplateRepository,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectAuditService auditService,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.structureTemplateRepository = structureTemplateRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.auditService = auditService;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public StructureViews.ProjectStructureTemplateSummaryView execute(String structureTemplateId, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectStructureTemplateAggregate structureTemplate = structureTemplateRepository.findById(structureTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureTemplate", structureTemplateId));
        if (!projectTemplateRepository.findAllByStructureTemplateIdOrderByFrameworkTypeAscVersionDesc(structureTemplateId).isEmpty()) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_TEMPLATE_IN_USE",
                    "Project structure template cannot be purged because one or more project templates still reference it.");
        }
        StructureViews.ProjectStructureTemplateSummaryView response = viewMapper.toStructureTemplateSummaryView(structureTemplate);
        structureTemplateRepository.deleteById(structureTemplateId);
        auditService.record(
                actor,
                "PROJECT_STRUCTURE_TEMPLATE_PURGED",
                actor != null ? actor.tenantId() : null,
                structureTemplateId,
                "PROJECT_STRUCTURE_TEMPLATE",
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "active", structureTemplate.active(),
                        "frameworkType", structureTemplate.frameworkType().name(),
                        "name", structureTemplate.name(),
                        "version", structureTemplate.version())));
        return response;
    }
}
