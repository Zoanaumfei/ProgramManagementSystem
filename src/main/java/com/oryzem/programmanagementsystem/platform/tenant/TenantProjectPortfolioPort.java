package com.oryzem.programmanagementsystem.platform.tenant;

import java.util.List;
import java.util.Set;

public interface TenantProjectPortfolioPort {

    List<ProgramReference> listProgramReferences();

    PurgeProgramsResult purgeOwnedPrograms(Set<String> ownerOrganizationIds);

    record ProgramReference(
            String ownerOrganizationId,
            Set<String> participantOrganizationIds,
            boolean hasActiveProjects) {
    }

    record PurgeProgramsResult(
            int purgedPrograms,
            int purgedDocuments) {
    }
}
