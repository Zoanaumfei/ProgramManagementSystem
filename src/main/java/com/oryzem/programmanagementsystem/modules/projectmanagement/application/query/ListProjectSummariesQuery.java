package com.oryzem.programmanagementsystem.modules.projectmanagement.application.query;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.*;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListProjectSummariesQuery {

    private final ProjectRepository projectRepository;
    private final ProjectOrganizationRepository organizationRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectViewMapper viewMapper;

    public ListProjectSummariesQuery(
            ProjectRepository projectRepository,
            ProjectOrganizationRepository organizationRepository,
            ProjectMemberRepository memberRepository,
            ProjectAuthorizationService authorizationService,
            ProjectViewMapper viewMapper) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
        this.viewMapper = viewMapper;
    }

    public List<ProjectViews.ProjectSummaryView> execute(AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        return visibleProjects(actor).stream()
                .filter(project -> authorizationService.canAccessProject(
                        project,
                        organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(project.id()),
                        memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(project.id()),
                        actor,
                        ProjectPermission.VIEW_PROJECT))
                .sorted(Comparator.comparing(ProjectAggregate::createdAt).reversed())
                .map(viewMapper::toSummary)
                .toList();
    }

    private List<ProjectAggregate> visibleProjects(AuthenticatedUser actor) {
        if (actor != null
                && actor.tenantType() == TenantType.INTERNAL
                && actor.roles() != null
                && actor.roles().contains(Role.ADMIN)) {
            return projectRepository.findAllOrderByCreatedAtDescIdDesc();
        }
        return projectRepository.findAllByTenantIdOrderByCreatedAtDescIdDesc(actor.tenantId());
    }
}


