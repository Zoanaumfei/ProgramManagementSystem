package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.DeliverableSubmissionAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectDeliverableAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectMilestoneAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectStructureNodeAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectAuthorizationSnapshotFactory {

    private final ProjectActorContextResolver actorContextResolver;

    public ProjectAuthorizationSnapshotFactory(ProjectActorContextResolver actorContextResolver) {
        this.actorContextResolver = actorContextResolver;
    }

    public ProjectAuthorizationSnapshot projectSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser actor,
            ProjectPermission permission) {
        return new ProjectAuthorizationSnapshot(
                project.tenantId(),
                project.id(),
                project.leadOrganizationId(),
                project.visibilityScope(),
                actorContextResolver.resolve(organizations, members, actor),
                permission);
    }

    public ProjectDeliverableAuthorizationSnapshot deliverableSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectDeliverableAggregate deliverable,
            com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser actor,
            ProjectPermission permission) {
        return new ProjectDeliverableAuthorizationSnapshot(
                project.tenantId(),
                project.id(),
                deliverable.id(),
                project.leadOrganizationId(),
                deliverable.visibilityScope(),
                deliverable.responsibleOrganizationId(),
                deliverable.responsibleUserId(),
                deliverable.approverOrganizationId(),
                deliverable.approverUserId(),
                deliverable.status(),
                actorContextResolver.resolve(organizations, members, actor),
                permission);
    }

    public ProjectMilestoneAuthorizationSnapshot milestoneSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectMilestoneAggregate milestone,
            com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser actor,
            ProjectPermission permission) {
        return new ProjectMilestoneAuthorizationSnapshot(
                project.tenantId(),
                project.id(),
                milestone.id(),
                project.leadOrganizationId(),
                milestone.visibilityScope(),
                milestone.ownerOrganizationId(),
                actorContextResolver.resolve(organizations, members, actor),
                permission);
    }

    public ProjectStructureNodeAuthorizationSnapshot structureNodeSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectStructureNodeAggregate node,
            com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser actor,
            ProjectPermission permission) {
        return new ProjectStructureNodeAuthorizationSnapshot(
                project.tenantId(),
                project.id(),
                node.id(),
                project.leadOrganizationId(),
                node.visibilityScope(),
                node.ownerOrganizationId(),
                node.responsibleUserId(),
                actorContextResolver.resolve(organizations, members, actor),
                permission);
    }

    public DeliverableSubmissionAuthorizationSnapshot submissionSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectDeliverableAggregate deliverable,
            DeliverableSubmissionAggregate submission,
            com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser actor,
            ProjectPermission permission) {
        return new DeliverableSubmissionAuthorizationSnapshot(
                deliverableSnapshot(project, organizations, members, deliverable, actor, permission),
                submission.id(),
                submission.status(),
                permission);
    }
}
