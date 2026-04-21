package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentAdministrationFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentContextRef;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPurgeSummary;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeArtifactPort;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPurgeIntentRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProjectPurgeIntentUseCase {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final ProjectAuthorizationService authorizationService;
    private final ProjectPurgeAuthorizationService purgeAuthorizationService;
    private final ProjectRepository projectRepository;
    private final ProjectPurgeArtifactPort artifactPort;
    private final DocumentAdministrationFacade documentAdministrationFacade;
    private final ProjectPurgeIntentRepository purgeIntentRepository;
    private final ProjectAuditService auditService;
    private final Clock clock;

    public CreateProjectPurgeIntentUseCase(
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
    public ProjectPurgeViews.ProjectPurgeIntentView execute(String projectId, String reason, AuthenticatedUser actor) {
        authorizationService.assertEnabled();
        purgeAuthorizationService.assertCanPurge(actor);
        String normalizedReason = normalizeReason(reason);
        ProjectAggregate project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        ProjectPurgeArtifactPort.ProjectPurgePlan plan = artifactPort.loadPlan(projectId);
        DocumentPurgeSummary documentSummary = documentAdministrationFacade.summarizeTrackedDocuments(documentContexts(projectId, plan));
        Instant now = Instant.now(clock);
        ProjectPurgeIntent intent = purgeIntentRepository.save(new ProjectPurgeIntent(
                UUID.randomUUID().toString(),
                projectId,
                actor != null ? actor.userId() : null,
                actor != null ? actor.username() : null,
                normalizedReason,
                ProjectPurgeIntentStatus.PENDING,
                now,
                now.plus(DEFAULT_TTL),
                null));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("expiresAt", intent.expiresAt());
        metadata.put("projectCode", project.code());
        metadata.put("projectName", project.name());
        metadata.put("reason", normalizedReason);
        metadata.put("impact", toImpactView(plan, documentSummary));
        auditService.record(
                actor,
                "PROJECT_PURGE_INTENT_CREATED",
                project.tenantId(),
                projectId,
                "PROJECT",
                metadata);

        return new ProjectPurgeViews.ProjectPurgeIntentView(
                project.id(),
                project.code(),
                project.name(),
                normalizedReason,
                intent.token(),
                intent.expiresAt(),
                true,
                toImpactView(plan, documentSummary));
    }

    static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A purge reason is required.");
        }
        return reason.trim();
    }

    static ProjectPurgeViews.ProjectPurgeImpactView toImpactView(
            ProjectPurgeArtifactPort.ProjectPurgePlan plan,
            DocumentPurgeSummary documentSummary) {
        return new ProjectPurgeViews.ProjectPurgeImpactView(
                plan.organizationCount(),
                plan.memberCount(),
                plan.phaseCount(),
                plan.milestoneCount(),
                plan.deliverableCount(),
                plan.submissionCount(),
                plan.submissionDocumentLinkCount(),
                plan.structureNodeCount(),
                documentSummary.documentCount(),
                documentSummary.storageObjectCount());
    }

    static java.util.List<DocumentContextRef> documentContexts(
            String projectId,
            ProjectPurgeArtifactPort.ProjectPurgePlan plan) {
        java.util.ArrayList<DocumentContextRef> contexts = new java.util.ArrayList<>();
        contexts.add(new DocumentContextRef(DocumentContextType.PROJECT, projectId));
        plan.deliverableIds().forEach(deliverableId ->
                contexts.add(new DocumentContextRef(DocumentContextType.PROJECT_DELIVERABLE, deliverableId)));
        plan.submissionIds().forEach(submissionId ->
                contexts.add(new DocumentContextRef(DocumentContextType.PROJECT_DELIVERABLE_SUBMISSION, submissionId)));
        return java.util.List.copyOf(contexts);
    }
}
