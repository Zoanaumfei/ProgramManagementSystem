package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.DeliverableTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectDeliverableStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectMilestoneTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationRoleType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectOrganizationAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectPhaseTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelDefinition;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureLevelTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureNodeStatus;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectStructureRules;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAppliesToType;
import com.oryzem.programmanagementsystem.modules.projectmanagement.domain.ProjectTemplateAggregate;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.DeliverableTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectDeliverableRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectMilestoneTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectPhaseTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureLevelTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectStructureNodeRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectTemplateRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectIds;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectTemplateInstantiationService {

    private final ProjectPhaseTemplateRepository phaseTemplateRepository;
    private final ProjectMilestoneTemplateRepository milestoneTemplateRepository;
    private final DeliverableTemplateRepository deliverableTemplateRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectStructureLevelTemplateRepository structureLevelTemplateRepository;
    private final ProjectStructureNodeRepository structureNodeRepository;
    private final ProjectPhaseRepository phaseRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectDeliverableRepository deliverableRepository;
    private final Clock clock;

    public ProjectTemplateInstantiationService(
            ProjectPhaseTemplateRepository phaseTemplateRepository,
            ProjectMilestoneTemplateRepository milestoneTemplateRepository,
            DeliverableTemplateRepository deliverableTemplateRepository,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectStructureLevelTemplateRepository structureLevelTemplateRepository,
            ProjectStructureNodeRepository structureNodeRepository,
            ProjectPhaseRepository phaseRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectDeliverableRepository deliverableRepository,
            Clock clock) {
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.deliverableTemplateRepository = deliverableTemplateRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.structureLevelTemplateRepository = structureLevelTemplateRepository;
        this.structureNodeRepository = structureNodeRepository;
        this.phaseRepository = phaseRepository;
        this.milestoneRepository = milestoneRepository;
        this.deliverableRepository = deliverableRepository;
        this.clock = clock;
    }

    @Transactional
    public void instantiate(ProjectAggregate project, List<ProjectOrganizationAggregate> organizations) {
        InstantiationContext context = prepareInstantiationContext(project, organizations);
        instantiateArtifactsForNode(project, context.rootNode(), context.organizationsByRole(), context.projectPhaseIdsByTemplateId(), context.baseDate());
    }

    @Transactional
    public void instantiateForNode(ProjectAggregate project, ProjectStructureNodeAggregate node, List<ProjectOrganizationAggregate> organizations) {
        InstantiationContext context = prepareNodeInstantiationContext(project, node, organizations);
        instantiateArtifactsForNode(project, context.rootNode(), context.organizationsByRole(), context.projectPhaseIdsByTemplateId(), context.baseDate());
    }

    private InstantiationContext prepareInstantiationContext(
            ProjectAggregate project,
            List<ProjectOrganizationAggregate> organizations) {
        LocalDate baseDate = resolveBaseDate(project);
        return new InstantiationContext(
                ensureRootStructureNode(project),
                organizationsByRole(organizations),
                ensureProjectPhases(project, baseDate),
                baseDate);
    }

    private InstantiationContext prepareNodeInstantiationContext(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            List<ProjectOrganizationAggregate> organizations) {
        LocalDate baseDate = resolveBaseDate(project);
        return new InstantiationContext(
                node,
                organizationsByRole(organizations),
                resolveProjectPhaseIdsByTemplateId(project),
                baseDate);
    }

    private LocalDate resolveBaseDate(ProjectAggregate project) {
        return project.plannedStartDate() != null ? project.plannedStartDate() : LocalDate.now(clock);
    }

    private ProjectStructureNodeAggregate ensureRootStructureNode(ProjectAggregate project) {
        String rootNodeId = ProjectIds.rootProjectStructureNodeId(project.id());
        return structureNodeRepository.findById(rootNodeId)
                .orElseGet(() -> structureNodeRepository.save(ProjectStructureNodeAggregate.createRoot(
                        rootNodeId,
                        project.id(),
                        resolveRootLevelTemplateId(project),
                        project.name(),
                        project.code(),
                        project.leadOrganizationId(),
                        project.createdByUserId(),
                        mapProjectStatus(project),
                        project.visibilityScope())));
    }

    private String resolveRootLevelTemplateId(ProjectAggregate project) {
        ProjectTemplateAggregate template = projectTemplateRepository.findById(project.templateId())
                .orElseThrow(() -> new IllegalStateException("Project template not found for project " + project.id()));
        List<ProjectStructureLevelTemplateAggregate> levels = structureLevelTemplateRepository.findAllByStructureTemplateIdOrderBySequenceNoAsc(template.structureTemplateId());
        return ProjectStructureRules.rootLevel(levels.stream().map(this::toDefinition).toList()).id();
    }

    private ProjectStructureNodeStatus mapProjectStatus(ProjectAggregate project) {
        return switch (project.status()) {
            case ACTIVE -> ProjectStructureNodeStatus.ACTIVE;
            case COMPLETED -> ProjectStructureNodeStatus.COMPLETED;
            case CANCELLED -> ProjectStructureNodeStatus.CANCELLED;
            case DRAFT, PLANNED, ON_HOLD -> ProjectStructureNodeStatus.PLANNED;
        };
    }

    private void instantiateArtifactsForNode(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            LocalDate baseDate) {
        Map<String, String> projectMilestoneIdsByTemplateId = instantiateMilestones(
                project,
                node,
                organizationsByRole,
                projectPhaseIdsByTemplateId,
                baseDate);
        instantiateDeliverables(
                project,
                node,
                organizationsByRole,
                projectPhaseIdsByTemplateId,
                projectMilestoneIdsByTemplateId,
                baseDate);
    }

    private Map<String, String> instantiateMilestones(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            LocalDate baseDate) {
        boolean rootNode = isRootNode(node);
        Map<String, String> projectMilestoneIdsByTemplateId = new HashMap<>();
        for (ProjectMilestoneTemplateAggregate milestoneTemplate : loadApplicableMilestoneTemplates(project, node, rootNode)) {
            String milestoneId = ProjectIds.newProjectMilestoneId();
            projectMilestoneIdsByTemplateId.put(milestoneTemplate.id(), milestoneId);
            milestoneRepository.save(buildMilestoneAggregate(
                    project,
                    node,
                    milestoneTemplate,
                    organizationsByRole,
                    projectPhaseIdsByTemplateId,
                    baseDate,
                    milestoneId));
        }
        return projectMilestoneIdsByTemplateId;
    }

    private void instantiateDeliverables(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            Map<String, String> projectMilestoneIdsByTemplateId,
            LocalDate baseDate) {
        boolean rootNode = isRootNode(node);
        for (DeliverableTemplateAggregate deliverableTemplate : loadApplicableDeliverableTemplates(project, node, rootNode)) {
            deliverableRepository.save(buildDeliverableAggregate(
                    project,
                    node,
                    deliverableTemplate,
                    organizationsByRole,
                    projectPhaseIdsByTemplateId,
                    projectMilestoneIdsByTemplateId,
                    baseDate));
        }
    }

    private List<ProjectMilestoneTemplateAggregate> loadApplicableMilestoneTemplates(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            boolean rootNode) {
        return milestoneTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(project.templateId()).stream()
                .filter(milestoneTemplate -> appliesToNode(
                        milestoneTemplate.appliesToType(),
                        milestoneTemplate.structureLevelTemplateId(),
                        node,
                        rootNode))
                .toList();
    }

    private List<DeliverableTemplateAggregate> loadApplicableDeliverableTemplates(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            boolean rootNode) {
        return deliverableTemplateRepository.findAllByTemplateIdOrderByPlannedDueOffsetDaysAscCodeAsc(project.templateId()).stream()
                .filter(deliverableTemplate -> appliesToNode(
                        deliverableTemplate.appliesToType(),
                        deliverableTemplate.structureLevelTemplateId(),
                        node,
                        rootNode))
                .toList();
    }

    private ProjectMilestoneAggregate buildMilestoneAggregate(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            ProjectMilestoneTemplateAggregate milestoneTemplate,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            LocalDate baseDate,
            String milestoneId) {
        return new ProjectMilestoneAggregate(
                milestoneId,
                project.id(),
                node.id(),
                resolvePhaseId(milestoneTemplate.phaseTemplateId(), projectPhaseIdsByTemplateId),
                milestoneTemplate.code(),
                milestoneTemplate.name(),
                milestoneTemplate.sequenceNo(),
                baseDate.plusDays(milestoneTemplate.plannedOffsetDays()),
                null,
                ProjectMilestoneStatus.NOT_STARTED,
                resolveOrganizationId(milestoneTemplate.ownerOrganizationRole(), organizationsByRole, project),
                milestoneTemplate.visibilityScope(),
                0L);
    }

    private ProjectDeliverableAggregate buildDeliverableAggregate(
            ProjectAggregate project,
            ProjectStructureNodeAggregate node,
            DeliverableTemplateAggregate deliverableTemplate,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            Map<String, String> projectMilestoneIdsByTemplateId,
            LocalDate baseDate) {
        return new ProjectDeliverableAggregate(
                ProjectIds.newDeliverableId(),
                project.id(),
                node.id(),
                resolvePhaseId(deliverableTemplate.phaseTemplateId(), projectPhaseIdsByTemplateId),
                resolveMilestoneId(deliverableTemplate.milestoneTemplateId(), projectMilestoneIdsByTemplateId),
                deliverableTemplate.code(),
                deliverableTemplate.name(),
                deliverableTemplate.description(),
                deliverableTemplate.deliverableType() != null ? deliverableTemplate.deliverableType() : DeliverableType.DOCUMENT_PACKAGE,
                resolveOrganizationId(deliverableTemplate.responsibleOrganizationRole(), organizationsByRole, project),
                null,
                resolveOrganizationId(deliverableTemplate.approverOrganizationRole(), organizationsByRole, project),
                null,
                deliverableTemplate.requiredDocument(),
                baseDate.plusDays(deliverableTemplate.plannedDueOffsetDays()),
                null,
                null,
                ProjectDeliverableStatus.NOT_STARTED,
                deliverableTemplate.priority(),
                deliverableTemplate.visibilityScope(),
                0L);
    }

    private boolean appliesToNode(
            ProjectTemplateAppliesToType appliesToType,
            String structureLevelTemplateId,
            ProjectStructureNodeAggregate node,
            boolean rootNode) {
        if (appliesToType == ProjectTemplateAppliesToType.ROOT_NODE) {
            return rootNode;
        }
        return structureLevelTemplateId != null && structureLevelTemplateId.equals(node.levelTemplateId());
    }

    private boolean isRootNode(ProjectStructureNodeAggregate node) {
        return node.parentNodeId() == null;
    }

    private String resolvePhaseId(String phaseTemplateId, Map<String, String> projectPhaseIdsByTemplateId) {
        return phaseTemplateId != null ? projectPhaseIdsByTemplateId.get(phaseTemplateId) : null;
    }

    private String resolveMilestoneId(String milestoneTemplateId, Map<String, String> projectMilestoneIdsByTemplateId) {
        return milestoneTemplateId != null ? projectMilestoneIdsByTemplateId.get(milestoneTemplateId) : null;
    }

    private Map<ProjectOrganizationRoleType, String> organizationsByRole(List<ProjectOrganizationAggregate> organizations) {
        Map<ProjectOrganizationRoleType, String> organizationsByRole = new HashMap<>();
        for (ProjectOrganizationAggregate organization : organizations) {
            organizationsByRole.putIfAbsent(organization.roleType(), organization.organizationId());
        }
        return organizationsByRole;
    }

    private Map<String, String> ensureProjectPhases(ProjectAggregate project, LocalDate baseDate) {
        List<ProjectPhaseAggregate> existing = phaseRepository.findAllByProjectIdOrderBySequenceNoAsc(project.id());
        if (!existing.isEmpty()) {
            return resolveProjectPhaseIdsByTemplateId(project);
        }
        Map<String, String> projectPhaseIdsByTemplateId = new HashMap<>();
        for (ProjectPhaseTemplateAggregate phaseTemplate : phaseTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(project.templateId())) {
            String phaseId = ProjectIds.newProjectPhaseId();
            projectPhaseIdsByTemplateId.put(phaseTemplate.id(), phaseId);
            LocalDate plannedStart = phaseTemplate.plannedStartOffsetDays() != null
                    ? baseDate.plusDays(phaseTemplate.plannedStartOffsetDays())
                    : baseDate;
            LocalDate plannedEnd = baseDate.plusDays(phaseTemplate.plannedEndOffsetDays());
            phaseRepository.save(ProjectPhaseAggregate.create(
                    phaseId,
                    project.id(),
                    phaseTemplate.name(),
                    phaseTemplate.sequenceNo(),
                    plannedStart,
                    plannedEnd));
        }
        return projectPhaseIdsByTemplateId;
    }

    private Map<String, String> resolveProjectPhaseIdsByTemplateId(ProjectAggregate project) {
        List<ProjectPhaseTemplateAggregate> templates = phaseTemplateRepository.findAllByTemplateIdOrderBySequenceNoAsc(project.templateId());
        List<ProjectPhaseAggregate> phases = phaseRepository.findAllByProjectIdOrderBySequenceNoAsc(project.id());
        if (templates.size() != phases.size()) {
            throw new IllegalStateException("Project phase/template mismatch for project " + project.id());
        }
        Map<String, String> projectPhaseIdsByTemplateId = new HashMap<>();
        for (int index = 0; index < templates.size(); index++) {
            projectPhaseIdsByTemplateId.put(templates.get(index).id(), phases.get(index).id());
        }
        return projectPhaseIdsByTemplateId;
    }

    private String resolveOrganizationId(
            ProjectOrganizationRoleType roleType,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            ProjectAggregate project) {
        if (roleType == null) {
            return project.leadOrganizationId();
        }
        return organizationsByRole.getOrDefault(roleType, switch (roleType) {
            case LEAD -> project.leadOrganizationId();
            case CUSTOMER -> project.customerOrganizationId();
            case SUPPLIER, PARTNER -> project.leadOrganizationId();
        });
    }

    private ProjectStructureLevelDefinition toDefinition(ProjectStructureLevelTemplateAggregate entity) {
        return new ProjectStructureLevelDefinition(
                entity.id(),
                entity.sequenceNo(),
                entity.allowsChildren(),
                entity.allowsMilestones(),
                entity.allowsDeliverables());
    }

    private record InstantiationContext(
            ProjectStructureNodeAggregate rootNode,
            Map<ProjectOrganizationRoleType, String> organizationsByRole,
            Map<String, String> projectPhaseIdsByTemplateId,
            LocalDate baseDate) {
    }
}

