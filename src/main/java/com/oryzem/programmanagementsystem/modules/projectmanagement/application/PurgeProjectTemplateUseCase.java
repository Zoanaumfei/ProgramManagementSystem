package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurgeProjectTemplateUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectStructureTemplateAdministrationService administrationService;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuditService auditService;
    private final ProjectViewMapper viewMapper;

    public PurgeProjectTemplateUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectStructureTemplateAdministrationService administrationService,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectRepository projectRepository,
            ProjectAuditService auditService,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.administrationService = administrationService;
        this.projectTemplateRepository = projectTemplateRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public TemplateViews.ProjectTemplateSummaryView execute(String templateId, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        administrationService.authorizeManagement(actor);
        ProjectTemplateAggregate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTemplate", templateId));
        if (template.isDefault()) {
            throw new BusinessRuleException(
                    "PROJECT_TEMPLATE_DEFAULT_CANNOT_BE_PURGED",
                    "Default project templates cannot be purged. Assign another default or unset the default flag first.");
        }
        if (projectRepository.existsByTemplateId(templateId)) {
            throw new BusinessRuleException(
                    "PROJECT_TEMPLATE_IN_USE",
                    "Project template cannot be purged because it is already linked to one or more projects.");
        }
        TemplateViews.ProjectTemplateSummaryView response = viewMapper.toProjectTemplateSummaryView(template);
        projectTemplateRepository.deleteById(templateId);
        auditService.record(
                actor,
                "PROJECT_TEMPLATE_PURGED",
                actor != null ? actor.tenantId() : null,
                templateId,
                "PROJECT_TEMPLATE",
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "frameworkType", template.frameworkType().name(),
                        "isDefault", template.isDefault(),
                        "name", template.name(),
                        "status", template.status().name(),
                        "structureTemplateId", template.structureTemplateId(),
                        "version", template.version())));
        return response;
    }
}
