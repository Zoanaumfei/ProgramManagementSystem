package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentAdministrationFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPurgeSummary;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeArtifactPort;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeIntentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurgeProjectUseCase {

    public static final String REQUIRED_CONFIRMATION_TEXT = "PURGE PROJECT";

    private final ProjectAuthorizationService authorizationService;
    private final ProjectPurgeAuthorizationService purgeAuthorizationService;
    private final ProjectRepository projectRepository;
    private final ProjectPurgeArtifactPort artifactPort;
    private final DocumentAdministrationFacade documentAdministrationFacade;
    private final ProjectPurgeIntentRepository purgeIntentRepository;
    private final ProjectAuditService auditService;
    private final Clock clock;

    public PurgeProjectUseCase(
            ProjectAuthorizationService authorizationService,
            ProjectPurgeAuthorizationService purgeAuthorizationService,
            ProjectRepository projectRepository,
            ProjectPurgeArtifactPort artifactPort,
            DocumentAdministrationFacade documentAdministrationFacade,
            ProjectPurgeIntentRepository purgeIntentRepository,
            ProjectAuditService auditService,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.purgeAuthorizationService = purgeAuthorizationService;
        this.projectRepository = projectRepository;
        this.artifactPort = artifactPort;
        this.documentAdministrationFacade = documentAdministrationFacade;
        this.purgeIntentRepository = purgeIntentRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ProjectPurgeViews.ProjectPurgeResultView execute(String projectId, ExecuteProjectPurgeCommand command, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        purgeAuthorizationService.assertCanPurge(actor);
        String normalizedReason = CreateProjectPurgeIntentUseCase.normalizeReason(command.reason());
        ProjectAggregate project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        ProjectPurgeIntent intent = purgeIntentRepository.findByToken(command.purgeToken())
                .orElseThrow(() -> new BusinessRuleException(
                        "PROJECT_PURGE_TOKEN_INVALID",
                        "The provided purge token is invalid or no longer available."));

        Instant now = Instant.now(clock);
        if (!projectId.equals(intent.projectId())) {
            throw new BusinessRuleException(
                    "PROJECT_PURGE_TOKEN_MISMATCH",
                    "The provided purge token does not belong to the informed project.");
        }
        if (intent.status() == ProjectPurgeIntentStatus.CONSUMED) {
            throw new BusinessRuleException(
                    "PROJECT_PURGE_TOKEN_ALREADY_CONSUMED",
                    "This purge token has already been consumed.");
        }
        if (intent.isExpired(now) || intent.status() == ProjectPurgeIntentStatus.EXPIRED) {
            purgeIntentRepository.save(intent.markExpired());
            throw new BusinessRuleException(
                    "PROJECT_PURGE_TOKEN_EXPIRED",
                    "The purge token has expired. Create a new purge intent and confirm again.");
        }
        if (!intent.reason().equals(normalizedReason)) {
            throw new BusinessRuleException(
                    "PROJECT_PURGE_REASON_MISMATCH",
                    "The provided reason does not match the original purge intent.");
        }
        if (!command.confirm()) {
            throw new BusinessRuleException(
                    "PROJECT_PURGE_CONFIRMATION_REQUIRED",
                    "Set confirm=true to execute the project purge.");
        }
        if (!REQUIRED_CONFIRMATION_TEXT.equals(command.confirmationText())) {
            throw new BusinessRuleException(
                    "PROJECT_PURGE_CONFIRMATION_TEXT_INVALID",
                    "Type the exact confirmation text to purge the project.");
        }

        ProjectPurgeArtifactPort.ProjectPurgePlan plan = artifactPort.loadPlan(projectId);
        DocumentPurgeSummary documentSummary = documentAdministrationFacade.summarizeTrackedDocuments(
                CreateProjectPurgeIntentUseCase.documentContexts(projectId, plan));
        LinkedHashMap<String, Object> startedMetadata = new LinkedHashMap<>();
        startedMetadata.put("projectCode", project.code());
        startedMetadata.put("projectName", project.name());
        startedMetadata.put("reason", normalizedReason);
        startedMetadata.put("impact", CreateProjectPurgeIntentUseCase.toImpactView(plan, documentSummary));
        auditService.record(
                actor,
                "PROJECT_PURGE_EXECUTION_STARTED",
                project.tenantId(),
                projectId,
                "PROJECT",
                startedMetadata);

        documentAdministrationFacade.purgeTrackedDocuments(CreateProjectPurgeIntentUseCase.documentContexts(projectId, plan));
        artifactPort.purgeArtifacts(plan);
        purgeIntentRepository.save(intent.markConsumed(now));

        LinkedHashMap<String, Object> completedMetadata = new LinkedHashMap<>();
        completedMetadata.put("projectCode", project.code());
        completedMetadata.put("projectName", project.name());
        completedMetadata.put("reason", normalizedReason);
        completedMetadata.put("purgedAt", now);
        completedMetadata.put("impact", CreateProjectPurgeIntentUseCase.toImpactView(plan, documentSummary));
        auditService.record(
                actor,
                "PROJECT_PURGE_COMPLETED",
                project.tenantId(),
                projectId,
                "PROJECT",
                completedMetadata);

        return new ProjectPurgeViews.ProjectPurgeResultView(
                projectId,
                project.code(),
                project.name(),
                normalizedReason,
                "PURGED",
                now,
                CreateProjectPurgeIntentUseCase.toImpactView(plan, documentSummary));
    }

    public record ExecuteProjectPurgeCommand(
            String reason,
            String purgeToken,
            boolean confirm,
            String confirmationText) {
    }
}
