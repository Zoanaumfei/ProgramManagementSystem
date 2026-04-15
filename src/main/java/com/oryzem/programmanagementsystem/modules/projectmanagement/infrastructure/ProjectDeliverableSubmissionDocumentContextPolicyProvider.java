package com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ProjectAuthorizationService;
import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.project-management", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectDeliverableSubmissionDocumentContextPolicyProvider implements DocumentContextPolicyProvider {

    private final SpringDataDeliverableSubmissionJpaRepository submissionRepository;
    private final SpringDataProjectDeliverableJpaRepository deliverableRepository;
    private final SpringDataProjectJpaRepository projectRepository;
    private final SpringDataProjectOrganizationJpaRepository organizationRepository;
    private final SpringDataProjectMemberJpaRepository memberRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectManagementProperties properties;

    public ProjectDeliverableSubmissionDocumentContextPolicyProvider(
            SpringDataDeliverableSubmissionJpaRepository submissionRepository,
            SpringDataProjectDeliverableJpaRepository deliverableRepository,
            SpringDataProjectJpaRepository projectRepository,
            SpringDataProjectOrganizationJpaRepository organizationRepository,
            SpringDataProjectMemberJpaRepository memberRepository,
            ProjectAuthorizationService authorizationService,
            ProjectManagementProperties properties) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
        this.properties = properties;
    }

    @Override
    public DocumentContextType supports() {
        return DocumentContextType.PROJECT_DELIVERABLE_SUBMISSION;
    }

    @Override
    public DocumentContextPolicy resolve(String contextId, AuthenticatedUser actor) {
        DeliverableSubmissionEntity submission = submissionRepository.findById(contextId).orElse(null);
        if (submission == null) {
            return new DocumentContextPolicy(false, false, false, null, false, false, false, false);
        }
        ProjectDeliverableEntity deliverable = deliverableRepository.findById(submission.getDeliverableId()).orElse(null);
        if (deliverable == null) {
            return new DocumentContextPolicy(false, false, false, null, false, false, false, false);
        }
        ProjectEntity project = projectRepository.findById(deliverable.getProjectId()).orElse(null);
        if (project == null) {
            return new DocumentContextPolicy(false, false, false, null, false, false, false, false);
        }
        var organizations = organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(project.getId()).stream()
                .map(entity -> new ProjectOrganizationAggregate(entity.getId(), entity.getProjectId(), entity.getOrganizationId(), entity.getRoleType(), entity.getJoinedAt(), entity.isActive()))
                .toList();
        var members = memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(project.getId()).stream()
                .map(entity -> new ProjectMemberAggregate(entity.getId(), entity.getProjectId(), entity.getUserId(), entity.getOrganizationId(), entity.getProjectRole(), entity.isActive(), entity.getAssignedAt()))
                .toList();
        boolean canUpload = authorizationService.canAccessSubmission(project.toDomain(), organizations, members, deliverable.toDomain(), submission.toDomain(), actor, ProjectPermission.UPLOAD_DOCUMENT)
                && (submission.getStatus() == DeliverableSubmissionStatus.SUBMITTED || submission.getStatus() == DeliverableSubmissionStatus.UNDER_REVIEW);
        return new DocumentContextPolicy(
                true,
                submission.getStatus() != DeliverableSubmissionStatus.CANCELLED,
                properties.isEnabled() && properties.isDocumentsEnabled(),
                project.getLeadOrganizationId(),
                authorizationService.canAccessSubmission(project.toDomain(), organizations, members, deliverable.toDomain(), submission.toDomain(), actor, ProjectPermission.VIEW_DOCUMENT),
                canUpload,
                authorizationService.canAccessSubmission(project.toDomain(), organizations, members, deliverable.toDomain(), submission.toDomain(), actor, ProjectPermission.DOWNLOAD_DOCUMENT),
                authorizationService.canAccessSubmission(project.toDomain(), organizations, members, deliverable.toDomain(), submission.toDomain(), actor, ProjectPermission.DELETE_DOCUMENT));
    }
}
