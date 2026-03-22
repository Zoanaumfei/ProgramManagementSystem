package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
class PortfolioProgramService {

    private final ProgramRepository programRepository;
    private final ProjectManagementLookupService lookupService;
    private final ProjectManagementAccessService accessService;
    private final PortfolioProjectService projectService;

    PortfolioProgramService(
            ProgramRepository programRepository,
            ProjectManagementLookupService lookupService,
            ProjectManagementAccessService accessService,
            PortfolioProjectService projectService) {
        this.programRepository = programRepository;
        this.lookupService = lookupService;
        this.accessService = accessService;
        this.projectService = projectService;
    }

    List<ProgramSummaryResponse> listPrograms(AuthenticatedUser actor, String ownerOrganizationId) {
        accessService.assertCanViewPortfolio(actor, null);
        String normalizedOwnerOrganizationId = ProjectManagementValidationSupport.trimToNull(ownerOrganizationId);
        if (normalizedOwnerOrganizationId != null) {
            OrganizationLookup.OrganizationView ownerOrganization =
                    accessService.findPortfolioOrganization(normalizedOwnerOrganizationId);
            accessService.assertCanViewPortfolio(actor, ownerOrganization.id());
        }

        return accessService.visiblePrograms(actor).stream()
                .filter(program -> normalizedOwnerOrganizationId == null
                        || normalizedOwnerOrganizationId.equals(program.getOwnerOrganizationId()))
                .map(ProgramSummaryResponse::from)
                .toList();
    }

    ProgramDetailResponse getProgram(String programId, AuthenticatedUser actor) {
        ProgramEntity program = lookupService.findProgram(programId);
        accessService.assertCanViewProgram(actor, program);
        return ProgramDetailResponse.from(program, accessService::resolveOrganizationName);
    }

    ProgramDetailResponse createProgram(CreateProgramRequest request, AuthenticatedUser actor) {
        ProjectManagementValidationSupport.validateDateRange(
                request.plannedStartDate(),
                request.plannedEndDate(),
                "Program");
        if (programRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Program code already exists.");
        }
        if (request.initialProject() == null) {
            throw new IllegalArgumentException("Program requires an initial project.");
        }

        OrganizationLookup.OrganizationView ownerOrganization =
                accessService.findPortfolioOrganization(request.ownerOrganizationId());
        accessService.assertCanManageProgram(actor, ownerOrganization.id());
        accessService.assertPortfolioOrganization(ownerOrganization, "Owner organization");
        accessService.assertOrganizationIsActive(ownerOrganization, "Owner organization");
        accessService.assertOrganizationSetupComplete(ownerOrganization.id(), "Owner organization");
        ProgramEntity program = ProgramEntity.create(
                actor.username(),
                request.name().trim(),
                request.code().trim().toUpperCase(),
                ProjectManagementValidationSupport.trimToNull(request.description()),
                ProgramStatus.DRAFT,
                request.plannedStartDate(),
                request.plannedEndDate(),
                ownerOrganization.id());

        addParticipants(program, ownerOrganization, request.participants(), actor.username());
        ProjectEntity initialProject = projectService.buildProject(program, request.initialProject(), actor.username());
        program.addProject(initialProject, actor.username());

        return ProgramDetailResponse.from(programRepository.save(program), accessService::resolveOrganizationName);
    }

    private void addParticipants(
            ProgramEntity program,
            OrganizationLookup.OrganizationView ownerOrganization,
            List<CreateProgramParticipationRequest> requests,
            String actor) {
        Set<String> includedOrganizations = new LinkedHashSet<>();
        if (requests != null) {
            for (CreateProgramParticipationRequest request : requests) {
                OrganizationLookup.OrganizationView organization = accessService.findOrganization(request.organizationId());
                accessService.assertPortfolioOrganization(organization, "Participant organization");
                accessService.assertOrganizationIsActive(organization, "Participant organization");
                accessService.assertOrganizationsBelongToSameCustomer(ownerOrganization, organization);
                if (!includedOrganizations.add(organization.id())) {
                    throw new IllegalArgumentException("Program participants cannot repeat the same organization.");
                }

                program.addParticipant(ProgramParticipationEntity.create(
                        actor,
                        program,
                        organization.id(),
                        request.role(),
                        ProjectManagementValidationSupport.defaultValue(
                                request.status(),
                                ParticipationStatus.ACTIVE)), actor);
            }
        }

        if (!includedOrganizations.contains(ownerOrganization.id())) {
            program.addParticipant(ProgramParticipationEntity.create(
                    actor,
                    program,
                    ownerOrganization.id(),
                    ParticipationRole.INTERNAL,
                    ParticipationStatus.ACTIVE), actor);
        }
    }
}
