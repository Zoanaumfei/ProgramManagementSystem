package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.DeliverableSubmissionAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectDeliverableAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectMilestoneAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectStructureNodeAuthorizationSnapshot;
import com.oryzem.programmanagementsystem.modules.projectmanagement.config.ProjectManagementProperties;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableSubmissionAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableSubmissionRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.shared.FeatureTemporarilyUnavailableException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectAuthorizationService {

    private final ProjectManagementProperties properties;
    private final ProjectAccessLoader projectAccessLoader;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final AccessContextService accessContextService;
    private final ProjectAuditService auditService;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final ProjectMilestoneAccessPolicy milestoneAccessPolicy;
    private final ProjectDeliverableAccessPolicy deliverableAccessPolicy;
    private final DeliverableSubmissionAccessPolicy submissionAccessPolicy;
    private final ProjectStructureNodeAccessPolicy structureNodeAccessPolicy;
    private final ProjectAuthorizationSnapshotFactory snapshotFactory;

    @Autowired
    public ProjectAuthorizationService(
            ProjectManagementProperties properties,
            ProjectAccessLoader projectAccessLoader,
            ProjectDeliverableRepository deliverableRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectStructureNodeRepository structureNodeRepository,
            DeliverableSubmissionRepository submissionRepository,
            AccessContextService accessContextService,
            ProjectAuditService auditService,
            ProjectAccessPolicy projectAccessPolicy,
            ProjectMilestoneAccessPolicy milestoneAccessPolicy,
            ProjectDeliverableAccessPolicy deliverableAccessPolicy,
            DeliverableSubmissionAccessPolicy submissionAccessPolicy,
            ProjectStructureNodeAccessPolicy structureNodeAccessPolicy,
            ProjectAuthorizationSnapshotFactory snapshotFactory) {
        this.properties = properties;
        this.projectAccessLoader = projectAccessLoader;
        this.deliverableRepository = deliverableRepository;
        this.milestoneRepository = milestoneRepository;
        this.structureNodeRepository = structureNodeRepository;
        this.submissionRepository = submissionRepository;
        this.accessContextService = accessContextService;
        this.auditService = auditService;
        this.projectAccessPolicy = projectAccessPolicy;
        this.milestoneAccessPolicy = milestoneAccessPolicy;
        this.deliverableAccessPolicy = deliverableAccessPolicy;
        this.submissionAccessPolicy = submissionAccessPolicy;
        this.structureNodeAccessPolicy = structureNodeAccessPolicy;
        this.snapshotFactory = snapshotFactory;
    }

    public ProjectAuthorizationService(
            ProjectManagementProperties properties,
            com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository projectRepository,
            com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository organizationRepository,
            com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository memberRepository,
            ProjectDeliverableRepository deliverableRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectStructureNodeRepository structureNodeRepository,
            DeliverableSubmissionRepository submissionRepository,
            AccessContextService accessContextService,
            ProjectAuditService auditService,
            ProjectAccessPolicy projectAccessPolicy,
            ProjectMilestoneAccessPolicy milestoneAccessPolicy,
            ProjectDeliverableAccessPolicy deliverableAccessPolicy,
            DeliverableSubmissionAccessPolicy submissionAccessPolicy,
            ProjectStructureNodeAccessPolicy structureNodeAccessPolicy) {
        this(
                properties,
                new ProjectAccessLoader(projectRepository, organizationRepository, memberRepository),
                deliverableRepository,
                milestoneRepository,
                structureNodeRepository,
                submissionRepository,
                accessContextService,
                auditService,
                projectAccessPolicy,
                milestoneAccessPolicy,
                deliverableAccessPolicy,
                submissionAccessPolicy,
                structureNodeAccessPolicy,
                new ProjectAuthorizationSnapshotFactory(new ProjectActorContextResolver()));
    }

    public void assertEnabled() {
        if (!properties.isEnabled()) {
            throw new FeatureTemporarilyUnavailableException("Project management is currently disabled.");
        }
    }

    public ProjectAccess authorizeProject(String projectId, AuthenticatedUser actor, ProjectPermission permission) {
        assertEnabled();
        ProjectAccess access = projectAccessLoader.loadProjectAccess(projectId);
        ProjectAggregate project = access.project();
        List<ProjectOrganizationAggregate> organizations = access.organizations();
        List<ProjectMemberAggregate> members = access.members();
        if (!isProjectAllowed(project, organizations, members, actor, permission)) {
            deny(actor, project.tenantId(), projectId, "PROJECT_ACCESS_DENIED", permission.name());
        }
        return access;
    }

    public DeliverableAccess authorizeDeliverable(String projectId, String deliverableId, AuthenticatedUser actor, ProjectPermission permission) {
        ProjectAccess projectAccess = authorizeProject(projectId, actor, ProjectPermission.VIEW_PROJECT);
        ProjectDeliverableAggregate deliverable = deliverableRepository.findByIdAndProjectId(deliverableId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDeliverable", deliverableId));
        if (!isDeliverableAllowed(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), deliverable, actor, permission)) {
            deny(actor, projectAccess.project().tenantId(), deliverableId, "PROJECT_DELIVERABLE_ACCESS_DENIED", permission.name());
        }
        return new DeliverableAccess(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), deliverable);
    }

    public SubmissionAccess authorizeSubmission(String projectId, String deliverableId, String submissionId, AuthenticatedUser actor, ProjectPermission permission) {
        DeliverableAccess deliverableAccess = authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.VIEW_DELIVERABLE);
        DeliverableSubmissionAggregate submission = submissionRepository.findByIdAndDeliverableId(submissionId, deliverableId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliverableSubmission", submissionId));
        if (!isSubmissionAllowed(deliverableAccess, submission, actor, permission)) {
            deny(actor, deliverableAccess.project().tenantId(), submissionId, "PROJECT_SUBMISSION_ACCESS_DENIED", permission.name());
        }
        return new SubmissionAccess(deliverableAccess.project(), deliverableAccess.organizations(), deliverableAccess.members(), deliverableAccess.deliverable(), submission);
    }

    public StructureNodeAccess authorizeStructureNode(String projectId, String nodeId, AuthenticatedUser actor, ProjectPermission permission) {
        ProjectAccess projectAccess = authorizeProject(projectId, actor, permission);
        ProjectStructureNodeAggregate node = structureNodeRepository.findByIdAndProjectId(nodeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectStructureNode", nodeId));
        if (!structureNodeAccessPolicy.isAllowed(structureNodeSnapshot(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), node, actor, permission))) {
            deny(actor, projectAccess.project().tenantId(), nodeId, "PROJECT_STRUCTURE_NODE_ACCESS_DENIED", permission.name());
        }
        return new StructureNodeAccess(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), node);
    }

    public MilestoneAccess authorizeMilestone(String projectId, String milestoneId, AuthenticatedUser actor, ProjectPermission permission) {
        ProjectAccess projectAccess = authorizeProject(projectId, actor, permission);
        ProjectMilestoneAggregate milestone = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMilestone", milestoneId));
        if (!milestoneAccessPolicy.isAllowed(milestoneSnapshot(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), milestone, actor, permission))) {
            deny(actor, projectAccess.project().tenantId(), milestoneId, "PROJECT_MILESTONE_ACCESS_DENIED", permission.name());
        }
        return new MilestoneAccess(projectAccess.project(), projectAccess.organizations(), projectAccess.members(), milestone);
    }

    public boolean canAccessProject(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, AuthenticatedUser actor, ProjectPermission permission) {
        return isProjectAllowed(project, organizations, members, actor, permission);
    }

    public boolean canAccessDeliverable(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectDeliverableAggregate deliverable, AuthenticatedUser actor, ProjectPermission permission) {
        return isDeliverableAllowed(project, organizations, members, deliverable, actor, permission);
    }

    public boolean canAccessMilestone(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectMilestoneAggregate milestone, AuthenticatedUser actor, ProjectPermission permission) {
        if (actor == null) {
            return false;
        }
        if (isInternalPrivileged(actor)) {
            return true;
        }
        if (!sameTenant(project.tenantId(), actor.tenantId())) {
            return false;
        }
        return milestoneAccessPolicy.isAllowed(milestoneSnapshot(project, organizations, members, milestone, actor, permission));
    }

    public boolean canAccessSubmission(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectDeliverableAggregate deliverable, DeliverableSubmissionAggregate submission, AuthenticatedUser actor, ProjectPermission permission) {
        return isSubmissionAllowed(new DeliverableAccess(project, organizations, members, deliverable), submission, actor, permission);
    }

    public boolean canAccessStructureNode(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectStructureNodeAggregate node, AuthenticatedUser actor, ProjectPermission permission) {
        if (actor == null) {
            return false;
        }
        if (isInternalPrivileged(actor)) {
            return true;
        }
        if (!sameTenant(project.tenantId(), actor.tenantId())) {
            return false;
        }
        return structureNodeAccessPolicy.isAllowed(structureNodeSnapshot(project, organizations, members, node, actor, permission));
    }

    private boolean isProjectAllowed(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, AuthenticatedUser actor, ProjectPermission permission) {
        if (actor == null) {
            return false;
        }
        if (isInternalPrivileged(actor)) {
            return true;
        }
        if (!sameTenant(project.tenantId(), actor.tenantId())) {
            return false;
        }
        return projectAccessPolicy.isAllowed(projectSnapshot(project, organizations, members, actor, permission));
    }

    private boolean isDeliverableAllowed(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectDeliverableAggregate deliverable,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        if (actor == null) {
            return false;
        }
        if (isInternalPrivileged(actor)) {
            return true;
        }
        if (!sameTenant(project.tenantId(), actor.tenantId())) {
            return false;
        }
        return deliverableAccessPolicy.isAllowed(deliverableSnapshot(project, organizations, members, deliverable, actor, permission));
    }

    private boolean isSubmissionAllowed(DeliverableAccess access, DeliverableSubmissionAggregate submission, AuthenticatedUser actor, ProjectPermission permission) {
        return submissionAccessPolicy.isAllowed(submissionSnapshot(
                access.project(),
                access.organizations(),
                access.members(),
                access.deliverable(),
                submission,
                actor,
                permission));
    }

    private ProjectAuthorizationSnapshot projectSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        return snapshotFactory.projectSnapshot(project, organizations, members, actor, permission);
    }

    private ProjectDeliverableAuthorizationSnapshot deliverableSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectDeliverableAggregate deliverable,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        return snapshotFactory.deliverableSnapshot(project, organizations, members, deliverable, actor, permission);
    }

    private ProjectMilestoneAuthorizationSnapshot milestoneSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectMilestoneAggregate milestone,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        return snapshotFactory.milestoneSnapshot(project, organizations, members, milestone, actor, permission);
    }

    private ProjectStructureNodeAuthorizationSnapshot structureNodeSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectStructureNodeAggregate node,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        return snapshotFactory.structureNodeSnapshot(project, organizations, members, node, actor, permission);
    }

    private DeliverableSubmissionAuthorizationSnapshot submissionSnapshot(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            ProjectDeliverableAggregate deliverable,
            DeliverableSubmissionAggregate submission,
            AuthenticatedUser actor,
            ProjectPermission permission) {
        return snapshotFactory.submissionSnapshot(project, organizations, members, deliverable, submission, actor, permission);
    }

    private boolean sameTenant(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equals(accessContextService.canonicalTenantId(right));
    }

    private boolean isInternalPrivileged(AuthenticatedUser actor) {
        return actor.tenantType() == com.oryzem.programmanagementsystem.platform.authorization.TenantType.INTERNAL
                && (actor.roles().contains(Role.ADMIN) || actor.roles().contains(Role.SUPPORT));
    }

    private void deny(AuthenticatedUser actor, String tenantId, String resourceId, String eventType, String permission) {
        auditService.record(actor, eventType, tenantId, resourceId, "PROJECT", java.util.Map.of("permission", permission, "result", "DENIED"));
        throw new AccessDeniedException("The authenticated user is not allowed to perform this project operation.");
    }

    public record ProjectAccess(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members) {
    }

    public record DeliverableAccess(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectDeliverableAggregate deliverable) {
    }

    public record SubmissionAccess(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectDeliverableAggregate deliverable, DeliverableSubmissionAggregate submission) {
    }

    public record MilestoneAccess(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectMilestoneAggregate milestone) {
    }

    public record StructureNodeAccess(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations, List<ProjectMemberAggregate> members, ProjectStructureNodeAggregate node) {
    }
}



