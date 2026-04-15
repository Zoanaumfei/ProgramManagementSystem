package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import org.springframework.stereotype.Component;

/**
 * Functional visibility rules for project-scoped artifacts.
 *
 * <p>Semantics:
 * <ul>
 *   <li>INTERNAL_ONLY: hidden from external project actors; only internal privileged users bypass this elsewhere.</li>
 *   <li>ALL_PROJECT_PARTICIPANTS: visible to every active project organization and project member.</li>
 *   <li>RESPONSIBLE_AND_APPROVER: visible to lead organization, coordinators/managers, and the assigned responsible/approver.</li>
 *   <li>LEAD_ONLY: visible to lead organization plus coordinators/managers.</li>
 * </ul>
 */
@Component
public class ProjectVisibilityPolicy {

    public boolean canViewMilestone(
            String leadOrganizationId,
            ProjectActorContext actorContext,
            ProjectVisibilityScope visibilityScope,
            String ownerOrganizationId) {
        if (visibilityScope == null) {
            return false;
        }
        return switch (visibilityScope) {
            case ALL_PROJECT_PARTICIPANTS -> actorContext.organizationParticipant() || !actorContext.memberRoles().isEmpty();
            case LEAD_ONLY -> actorContext.manager() || isLeadOrganization(leadOrganizationId, actorContext);
            case RESPONSIBLE_AND_APPROVER -> actorContext.manager()
                    || isLeadOrganization(leadOrganizationId, actorContext)
                    || actorContext.matchesAssignment(ownerOrganizationId, null);
            case INTERNAL_ONLY -> false;
        };
    }

    public boolean canViewStructureNode(
            String leadOrganizationId,
            ProjectActorContext actorContext,
            ProjectVisibilityScope visibilityScope,
            String ownerOrganizationId,
            String responsibleUserId) {
        if (visibilityScope == null) {
            return false;
        }
        return switch (visibilityScope) {
            case ALL_PROJECT_PARTICIPANTS -> actorContext.organizationParticipant() || !actorContext.memberRoles().isEmpty();
            case LEAD_ONLY -> actorContext.manager() || isLeadOrganization(leadOrganizationId, actorContext);
            case RESPONSIBLE_AND_APPROVER -> actorContext.manager()
                    || isLeadOrganization(leadOrganizationId, actorContext)
                    || actorContext.matchesAssignment(ownerOrganizationId, null)
                    || actorContext.matchesAssignment(null, responsibleUserId);
            case INTERNAL_ONLY -> false;
        };
    }

    public boolean canViewDeliverable(
            String leadOrganizationId,
            ProjectActorContext actorContext,
            ProjectVisibilityScope visibilityScope,
            String responsibleOrganizationId,
            String responsibleUserId,
            String approverOrganizationId,
            String approverUserId) {
        if (visibilityScope == null) {
            return false;
        }
        return switch (visibilityScope) {
            case ALL_PROJECT_PARTICIPANTS -> actorContext.organizationParticipant() || !actorContext.memberRoles().isEmpty();
            case LEAD_ONLY -> actorContext.manager() || isLeadOrganization(leadOrganizationId, actorContext);
            case RESPONSIBLE_AND_APPROVER -> actorContext.manager()
                    || isLeadOrganization(leadOrganizationId, actorContext)
                    || actorContext.matchesAssignment(responsibleOrganizationId, responsibleUserId)
                    || actorContext.matchesAssignment(approverOrganizationId, approverUserId);
            case INTERNAL_ONLY -> false;
        };
    }

    public boolean isLeadOrganization(String leadOrganizationId, ProjectActorContext actorContext) {
        return actorContext.actor() != null
                && leadOrganizationId != null
                && leadOrganizationId.equals(actorContext.actor().organizationId());
    }

    public boolean matchesAssignment(AuthenticatedUser actor, String organizationId, String userId) {
        if (actor == null) {
            return false;
        }
        return (organizationId != null && organizationId.equals(actor.organizationId()))
                || (userId != null && userId.equals(actor.userId()));
    }
}
