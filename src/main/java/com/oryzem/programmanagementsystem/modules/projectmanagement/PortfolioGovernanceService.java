package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
class PortfolioGovernanceService {

    private final ProgramRepository programRepository;
    private final ProjectManagementLookupService lookupService;
    private final ProjectManagementAccessService accessService;

    PortfolioGovernanceService(
            ProgramRepository programRepository,
            ProjectManagementLookupService lookupService,
            ProjectManagementAccessService accessService) {
        this.programRepository = programRepository;
        this.lookupService = lookupService;
        this.accessService = accessService;
    }

    OpenIssueResponse createOpenIssue(String programId, CreateOpenIssueRequest request, AuthenticatedUser actor) {
        ProgramEntity program = lookupService.findProgram(programId);
        accessService.assertCanManageProjectLayer(actor, program.getOwnerOrganizationId());
        OpenIssueEntity issue = OpenIssueEntity.create(
                actor.username(),
                program,
                request.title().trim(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProjectManagementValidationSupport.defaultValue(request.status(), OpenIssueStatus.OPEN),
                request.severity(),
                request.openedAt() != null ? request.openedAt() : OffsetDateTime.now());
        program.addOpenIssue(issue, actor.username());
        programRepository.save(program);
        return OpenIssueResponse.from(issue);
    }
}
