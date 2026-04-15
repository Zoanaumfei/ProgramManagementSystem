package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMemberRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectOrganizationRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMemberRole;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddProjectMemberUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectMemberRepository memberRepository;
    private final ProjectOrganizationRepository organizationRepository;
    private final AccessContextService accessContextService;
    private final UserRepository userRepository;
    private final ProjectViewMapper viewMapper;

    public AddProjectMemberUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectMemberRepository memberRepository,
            ProjectOrganizationRepository organizationRepository,
            AccessContextService accessContextService,
            UserRepository userRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.accessContextService = accessContextService;
        this.userRepository = userRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectMemberView execute(String projectId, AddProjectMemberCommand command, AuthenticatedUser actor) {
        authorizationService.authorizeProject(projectId, actor, ProjectPermission.ADD_MEMBER);
        userRepository.findById(command.userId()).orElseThrow(() -> new ResourceNotFoundException("User", command.userId()));
        organizationRepository.findByProjectIdAndOrganizationIdAndActiveTrue(projectId, command.organizationId())
                .orElseThrow(() -> new BusinessRuleException("PROJECT_MEMBER_ORGANIZATION_NOT_PARTICIPANT", "Member organization must already participate in the project."));
        if (!accessContextService.findUserIdsByOrganization(command.organizationId()).contains(command.userId())) {
            throw new BusinessRuleException("PROJECT_MEMBER_ORGANIZATION_MISMATCH", "The user does not belong to the informed organization.");
        }
        ProjectMemberAggregate existing = memberRepository.findByProjectIdAndUserIdAndActiveTrue(projectId, command.userId()).orElse(null);
        if (existing != null) {
            return viewMapper.toMemberView(existing);
        }
        ProjectMemberAggregate saved = memberRepository.save(new ProjectMemberAggregate(
                ProjectIds.newProjectMemberId(),
                projectId,
                command.userId(),
                command.organizationId(),
                command.projectRole() != null ? command.projectRole() : ProjectMemberRole.VIEWER,
                true,
                Instant.now()));
        return viewMapper.toMemberView(saved);
    }

    public record AddProjectMemberCommand(String userId, String organizationId, ProjectMemberRole projectRole) {
    }
}

