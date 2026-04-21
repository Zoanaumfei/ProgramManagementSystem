package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeArtifactPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectPurgeArtifactAdapter implements ProjectPurgeArtifactPort {

    private final SpringDataProjectOrganizationJpaRepository organizationRepository;
    private final SpringDataProjectMemberJpaRepository memberRepository;
    private final SpringDataProjectPhaseJpaRepository phaseRepository;
    private final SpringDataProjectMilestoneJpaRepository milestoneRepository;
    private final SpringDataProjectDeliverableJpaRepository deliverableRepository;
    private final SpringDataDeliverableSubmissionJpaRepository submissionRepository;
    private final SpringDataDeliverableSubmissionDocumentJpaRepository submissionDocumentRepository;
    private final SpringDataProjectStructureNodeJpaRepository structureNodeRepository;
    private final SpringDataProjectJpaRepository projectRepository;

    public JpaProjectPurgeArtifactAdapter(
            SpringDataProjectOrganizationJpaRepository organizationRepository,
            SpringDataProjectMemberJpaRepository memberRepository,
            SpringDataProjectPhaseJpaRepository phaseRepository,
            SpringDataProjectMilestoneJpaRepository milestoneRepository,
            SpringDataProjectDeliverableJpaRepository deliverableRepository,
            SpringDataDeliverableSubmissionJpaRepository submissionRepository,
            SpringDataDeliverableSubmissionDocumentJpaRepository submissionDocumentRepository,
            SpringDataProjectStructureNodeJpaRepository structureNodeRepository,
            SpringDataProjectJpaRepository projectRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.phaseRepository = phaseRepository;
        this.milestoneRepository = milestoneRepository;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.submissionDocumentRepository = submissionDocumentRepository;
        this.structureNodeRepository = structureNodeRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public ProjectPurgePlan loadPlan(String projectId) {
        List<ProjectDeliverableEntity> deliverables = deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId);
        List<DeliverableSubmissionEntity> submissions = new ArrayList<>();
        long submissionDocumentLinkCount = 0L;
        for (ProjectDeliverableEntity deliverable : deliverables) {
            List<DeliverableSubmissionEntity> deliverableSubmissions =
                    submissionRepository.findAllByDeliverableIdOrderBySubmissionNumberDesc(deliverable.getId());
            submissions.addAll(deliverableSubmissions);
            for (DeliverableSubmissionEntity submission : deliverableSubmissions) {
                submissionDocumentLinkCount += submissionDocumentRepository.findAllBySubmissionId(submission.getId()).size();
            }
        }

        return new ProjectPurgePlan(
                projectId,
                organizationRepository.findAllByProjectId(projectId).size(),
                memberRepository.findAllByProjectId(projectId).size(),
                phaseRepository.findAllByProjectIdOrderBySequenceNoAsc(projectId).size(),
                milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc(projectId).size(),
                deliverables.size(),
                submissions.size(),
                submissionDocumentLinkCount,
                structureNodeRepository.findAllByProjectIdOrderBySequenceNoAscIdAsc(projectId).size(),
                deliverables.stream().map(ProjectDeliverableEntity::getId).toList(),
                submissions.stream().map(DeliverableSubmissionEntity::getId).toList());
    }

    @Override
    public void purgeArtifacts(ProjectPurgePlan plan) {
        if (!plan.submissionIds().isEmpty()) {
            submissionDocumentRepository.deleteAllBySubmissionIdIn(plan.submissionIds());
        }
        if (!plan.deliverableIds().isEmpty()) {
            submissionRepository.deleteAllByDeliverableIdIn(plan.deliverableIds());
        }
        deliverableRepository.deleteAllByProjectId(plan.projectId());
        milestoneRepository.deleteAllByProjectId(plan.projectId());
        phaseRepository.deleteAllByProjectId(plan.projectId());
        memberRepository.deleteAllByProjectId(plan.projectId());
        organizationRepository.deleteAllByProjectId(plan.projectId());
        structureNodeRepository.deleteAllByProjectId(plan.projectId());
        projectRepository.deleteById(plan.projectId());
    }
}
