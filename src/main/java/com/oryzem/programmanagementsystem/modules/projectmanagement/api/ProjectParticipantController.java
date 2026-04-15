package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.AddProjectMemberUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.AddProjectOrganizationUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectMembersUseCase;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.ListProjectOrganizationsUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectParticipantController {

    private final ProjectApiSupport apiSupport;
    private final AddProjectOrganizationUseCase addProjectOrganizationUseCase;
    private final ListProjectOrganizationsUseCase listProjectOrganizationsUseCase;
    private final AddProjectMemberUseCase addProjectMemberUseCase;
    private final ListProjectMembersUseCase listProjectMembersUseCase;

    public ProjectParticipantController(
            ProjectApiSupport apiSupport,
            AddProjectOrganizationUseCase addProjectOrganizationUseCase,
            ListProjectOrganizationsUseCase listProjectOrganizationsUseCase,
            AddProjectMemberUseCase addProjectMemberUseCase,
            ListProjectMembersUseCase listProjectMembersUseCase) {
        this.apiSupport = apiSupport;
        this.addProjectOrganizationUseCase = addProjectOrganizationUseCase;
        this.listProjectOrganizationsUseCase = listProjectOrganizationsUseCase;
        this.addProjectMemberUseCase = addProjectMemberUseCase;
        this.listProjectMembersUseCase = listProjectMembersUseCase;
    }

    @PostMapping("/organizations")
    public ProjectParticipantDtos.ProjectOrganizationResponse addOrganization(Authentication authentication, @PathVariable String projectId, @Valid @RequestBody ProjectParticipantDtos.AddProjectOrganizationRequest request) {
        return ProjectParticipantDtos.ProjectOrganizationResponse.from(addProjectOrganizationUseCase.execute(projectId, new AddProjectOrganizationUseCase.AddProjectOrganizationCommand(request.organizationId(), request.roleType()), apiSupport.actor(authentication)));
    }

    @GetMapping("/organizations")
    public List<ProjectParticipantDtos.ProjectOrganizationResponse> listOrganizations(Authentication authentication, @PathVariable String projectId) {
        return listProjectOrganizationsUseCase.execute(projectId, apiSupport.actor(authentication)).stream().map(ProjectParticipantDtos.ProjectOrganizationResponse::from).toList();
    }

    @PostMapping("/members")
    public ProjectParticipantDtos.ProjectMemberResponse addMember(Authentication authentication, @PathVariable String projectId, @Valid @RequestBody ProjectParticipantDtos.AddProjectMemberRequest request) {
        return ProjectParticipantDtos.ProjectMemberResponse.from(addProjectMemberUseCase.execute(projectId, new AddProjectMemberUseCase.AddProjectMemberCommand(request.userId(), request.organizationId(), request.projectRole()), apiSupport.actor(authentication)));
    }

    @GetMapping("/members")
    public List<ProjectParticipantDtos.ProjectMemberResponse> listMembers(Authentication authentication, @PathVariable String projectId) {
        return listProjectMembersUseCase.execute(projectId, apiSupport.actor(authentication)).stream().map(ProjectParticipantDtos.ProjectMemberResponse::from).toList();
    }
}

