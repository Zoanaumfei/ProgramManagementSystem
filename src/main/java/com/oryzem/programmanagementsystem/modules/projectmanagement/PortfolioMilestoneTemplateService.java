package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class PortfolioMilestoneTemplateService {

    private final MilestoneTemplateRepository milestoneTemplateRepository;
    private final ProjectManagementAccessService accessService;

    PortfolioMilestoneTemplateService(
            MilestoneTemplateRepository milestoneTemplateRepository,
            ProjectManagementAccessService accessService) {
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.accessService = accessService;
    }

    List<MilestoneTemplateResponse> listMilestoneTemplates(AuthenticatedUser actor) {
        accessService.assertCanViewPortfolio(actor, null);
        return milestoneTemplateRepository.findAllByOrderByNameAsc().stream()
                .map(MilestoneTemplateResponse::from)
                .toList();
    }

    MilestoneTemplateResponse createMilestoneTemplate(
            CreateMilestoneTemplateRequest request,
            AuthenticatedUser actor) {
        accessService.assertCanConfigurePortfolio(actor);
        MilestoneTemplateEntity template = MilestoneTemplateEntity.create(
                actor.username(),
                request.name().trim(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProjectManagementValidationSupport.defaultValue(request.status(), MilestoneTemplateStatus.ACTIVE));

        request.items().stream()
                .sorted(Comparator.comparing(CreateMilestoneTemplateItemRequest::sortOrder))
                .forEach(itemRequest -> template.addItem(MilestoneTemplateItemEntity.create(
                        actor.username(),
                        template,
                        itemRequest.name().trim(),
                        itemRequest.sortOrder(),
                        itemRequest.required(),
                        itemRequest.offsetWeeks()), actor.username()));

        return MilestoneTemplateResponse.from(milestoneTemplateRepository.save(template));
    }
}
