package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectAccessLoader {

    private final ProjectRepository projectRepository;
    private final ProjectOrganizationRepository organizationRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectAccessLoader(
            ProjectRepository projectRepository,
            ProjectOrganizationRepository organizationRepository,
            ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
    }

    public ProjectAuthorizationService.ProjectAccess loadProjectAccess(String projectId) {
        ProjectAggregate project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        List<ProjectOrganizationAggregate> organizations = organizationRepository.findAllByProjectIdAndActiveTrueOrderByJoinedAtAsc(projectId);
        List<ProjectMemberAggregate> members = memberRepository.findAllByProjectIdAndActiveTrueOrderByAssignedAtAsc(projectId);
        return new ProjectAuthorizationService.ProjectAccess(project, organizations, members);
    }
}
