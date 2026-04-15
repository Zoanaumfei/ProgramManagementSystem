package com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.Set;

public record ProjectActorContext(
        AuthenticatedUser actor,
        boolean organizationParticipant,
        Set<ProjectMemberRole> memberRoles,
        boolean manager) {

    public boolean matchesOrganization(String organizationId) {
        return actor != null && organizationId != null && organizationId.equals(actor.organizationId());
    }

    public boolean matchesUser(String userId) {
        return actor != null && userId != null && userId.equals(actor.userId());
    }

    public boolean matchesAssignment(String organizationId, String userId) {
        return matchesOrganization(organizationId) || matchesUser(userId);
    }
}
