package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.documents.DocumentStorageObject;
import com.oryzem.programmanagementsystem.platform.documents.PortfolioDocumentStorageGateway;
import com.oryzem.programmanagementsystem.platform.tenant.TenantProjectPortfolioPort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
class ProjectManagementTenantPortfolioAdapter implements TenantProjectPortfolioPort {

    private final ProgramRepository programRepository;
    private final PortfolioDocumentStorageGateway documentStorageGateway;

    ProjectManagementTenantPortfolioAdapter(
            ProgramRepository programRepository,
            PortfolioDocumentStorageGateway documentStorageGateway) {
        this.programRepository = programRepository;
        this.documentStorageGateway = documentStorageGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramReference> listProgramReferences() {
        return programRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(program -> new ProgramReference(
                        program.getOwnerOrganizationId(),
                        program.getParticipants().stream()
                                .map(ProgramParticipationEntity::getOrganizationId)
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                        program.getProjects().stream().anyMatch(project -> project.getStatus() == ProjectStatus.ACTIVE)))
                .toList();
    }

    @Override
    public PurgeProgramsResult purgeOwnedPrograms(Set<String> ownerOrganizationIds) {
        List<ProgramEntity> programsToPurge = programRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(program -> ownerOrganizationIds.contains(program.getOwnerOrganizationId()))
                .toList();

        int purgedDocuments = 0;
        for (ProgramEntity program : programsToPurge) {
            for (ProjectEntity project : program.getProjects()) {
                for (ProductEntity product : project.getProducts()) {
                    for (ItemEntity item : product.getItems()) {
                        for (DeliverableEntity deliverable : item.getDeliverables()) {
                            for (DeliverableDocumentEntity document : deliverable.getDocuments()) {
                                documentStorageGateway.deleteObject(new DocumentStorageObject(
                                        document.getStorageBucket(),
                                        document.getStorageKey(),
                                        document.getContentType(),
                                        document.getFileName()));
                                purgedDocuments++;
                            }
                        }
                    }
                }
            }
        }

        programRepository.deleteAll(programsToPurge);
        return new PurgeProgramsResult(programsToPurge.size(), purgedDocuments);
    }
}
