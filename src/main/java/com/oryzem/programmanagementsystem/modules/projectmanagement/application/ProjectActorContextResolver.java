package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.authorization.ProjectActorContext;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProjectActorContextResolver {

    public ProjectActorContext resolve(
            List<ProjectOrganizationAggregate> organizations,
            List<ProjectMemberAggregate> members,
            AuthenticatedUser actor) {
        Set<ProjectMemberRole> memberRoles = actor == null
                ? Set.of()
                : members.stream()
                        .filter(member -> member.userId().equals(actor.userId()))
                        .map(ProjectMemberAggregate::projectRole)
                        .collect(Collectors.toSet());
        boolean organizationParticipant = actor != null
                && organizations.stream().anyMatch(entry -> entry.organizationId().equals(actor.organizationId()));
        boolean manager = memberRoles.contains(ProjectMemberRole.PROJECT_MANAGER)
                || memberRoles.contains(ProjectMemberRole.COORDINATOR);
        return new ProjectActorContext(actor, organizationParticipant, memberRoles, manager);
    }
}
