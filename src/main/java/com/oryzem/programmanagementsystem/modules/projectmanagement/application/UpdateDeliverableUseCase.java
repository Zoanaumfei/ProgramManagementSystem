package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPermission;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPriority;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectVisibilityScope;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDeliverableUseCase {

    private final ProjectAuthorizationService authorizationService;
    private final ProjectDeliverableRepository deliverableRepository;
    private final ProjectViewMapper viewMapper;

    public UpdateDeliverableUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectDeliverableRepository deliverableRepository,
            ProjectViewMapper viewMapper) {
        this.authorizationService = authorizationService;
        this.deliverableRepository = deliverableRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public ProjectViews.ProjectDeliverableView execute(String projectId, String deliverableId, UpdateDeliverableCommand command, AuthenticatedUser actor) {
        ProjectAuthorizationService.DeliverableAccess access = authorizationService.authorizeDeliverable(projectId, deliverableId, actor, ProjectPermission.EDIT_DELIVERABLE);
        if (command.version() != access.deliverable().version()) {
            throw new ConflictException("Project deliverable version mismatch.");
        }
        String nextCode = command.code() != null ? command.code() : access.deliverable().code();
        assertDeliverableCodeAvailable(projectId, access.deliverable().structureNodeId(), access.deliverable().id(), nextCode);
        ProjectDeliverableAggregate updated = access.deliverable().updateOperationalFields(
                nextCode,
                command.name() != null ? command.name() : access.deliverable().name(),
                command.description() != null ? command.description() : access.deliverable().description(),
                command.deliverableType() != null ? command.deliverableType() : access.deliverable().deliverableType(),
                command.responsibleOrganizationId() != null ? command.responsibleOrganizationId() : access.deliverable().responsibleOrganizationId(),
                command.responsibleUserId() != null ? command.responsibleUserId() : access.deliverable().responsibleUserId(),
                command.approverOrganizationId() != null ? command.approverOrganizationId() : access.deliverable().approverOrganizationId(),
                command.approverUserId() != null ? command.approverUserId() : access.deliverable().approverUserId(),
                command.requiredDocument() != null ? command.requiredDocument() : access.deliverable().requiredDocument(),
                command.plannedDueDate() != null ? command.plannedDueDate() : access.deliverable().plannedDueDate(),
                command.status(),
                command.priority(),
                command.visibilityScope());
        return viewMapper.toDeliverableView(deliverableRepository.save(updated));
    }

    private void assertDeliverableCodeAvailable(String projectId, String structureNodeId, String deliverableId, String code) {
        boolean duplicate = deliverableRepository.findAllByProjectIdAndStructureNodeIdOrderByPlannedDueDateAscIdAsc(projectId, structureNodeId).stream()
                .anyMatch(existing -> !existing.id().equals(deliverableId) && existing.code().equalsIgnoreCase(code.trim()));
        if (duplicate) {
            throw new BusinessRuleException(
                    "PROJECT_DELIVERABLE_CODE_ALREADY_EXISTS",
                    "Project deliverable code already exists for this project.",
                    Map.of("code", code.trim()));
        }
    }

    public record UpdateDeliverableCommand(
            String code,
            String name,
            String description,
            DeliverableType deliverableType,
            String responsibleOrganizationId,
            String responsibleUserId,
            String approverOrganizationId,
            String approverUserId,
            Boolean requiredDocument,
            LocalDate plannedDueDate,
            ProjectDeliverableStatus status,
            ProjectPriority priority,
            ProjectVisibilityScope visibilityScope,
            long version) {
    }
}

