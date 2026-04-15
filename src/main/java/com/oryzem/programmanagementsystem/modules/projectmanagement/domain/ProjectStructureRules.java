package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import com.oryzem.programmanagementsystem.platform.shared.BusinessRuleException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ProjectStructureRules {

    private ProjectStructureRules() {
    }

    public static ProjectStructureLevelDefinition rootLevel(List<ProjectStructureLevelDefinition> levels) {
        validateTemplate(levels);
        return levels.stream()
                .min(Comparator.comparingInt(ProjectStructureLevelDefinition::sequence))
                .orElseThrow(() -> new BusinessRuleException(
                        "PROJECT_STRUCTURE_TEMPLATE_EMPTY",
                        "Project structure templates must define at least one level."));
    }

    public static void validateTemplate(List<ProjectStructureLevelDefinition> levels) {
        validateOrderedLevels(levels);
        List<ProjectStructureLevelDefinition> ordered = levels.stream()
                .sorted(Comparator.comparingInt(ProjectStructureLevelDefinition::sequence))
                .toList();
        ProjectStructureLevelDefinition last = ordered.getLast();
        if (last.allowsChildren()) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_LAST_LEVEL_CANNOT_ALLOW_CHILDREN",
                    "The last project structure level cannot allow child nodes.",
                    Map.of("levelId", last.id(), "sequence", last.sequence()));
        }
    }

    public static void validateEditableTemplate(List<ProjectStructureLevelDefinition> levels) {
        validateOrderedLevels(levels);
    }

    private static void validateOrderedLevels(List<ProjectStructureLevelDefinition> levels) {
        if (levels == null || levels.isEmpty()) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_TEMPLATE_EMPTY",
                    "Project structure templates must define at least one level.");
        }
        List<ProjectStructureLevelDefinition> ordered = levels.stream()
                .sorted(Comparator.comparingInt(ProjectStructureLevelDefinition::sequence))
                .toList();
        for (int index = 0; index < ordered.size(); index++) {
            ProjectStructureLevelDefinition current = ordered.get(index);
            int expectedSequence = index + 1;
            if (current.sequence() != expectedSequence) {
                throw new BusinessRuleException(
                        "PROJECT_STRUCTURE_LEVEL_SEQUENCE_INVALID",
                        "Project structure level sequences must be contiguous and start at 1.",
                        Map.of("expectedSequence", expectedSequence, "actualSequence", current.sequence()));
            }
            boolean last = index == ordered.size() - 1;
            if (!last && !current.allowsChildren()) {
                throw new BusinessRuleException(
                        "PROJECT_STRUCTURE_INTERMEDIATE_LEVEL_MUST_ALLOW_CHILDREN",
                        "Every intermediate project structure level must allow child nodes.",
                        Map.of("levelId", current.id(), "sequence", current.sequence()));
            }
        }
    }

    public static ProjectStructureLevelDefinition childLevelOf(
            ProjectStructureLevelDefinition parent,
            List<ProjectStructureLevelDefinition> levels) {
        validateTemplate(levels);
        if (!parent.allowsChildren()) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_CHILDREN_NOT_ALLOWED",
                    "This structure node does not allow child nodes.");
        }
        return levels.stream()
                .filter(level -> level.sequence() == parent.sequence() + 1)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "PROJECT_STRUCTURE_LEVEL_NOT_AVAILABLE",
                        "No child structure level is configured after the parent level."));
    }

    public static void assertValidParentChild(
            ProjectStructureLevelDefinition parent,
            ProjectStructureLevelDefinition child) {
        if (!parent.allowsChildren()) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_CHILDREN_NOT_ALLOWED",
                    "Target parent does not allow child nodes.");
        }
        if (child.sequence() != parent.sequence() + 1) {
            throw new BusinessRuleException(
                    "PROJECT_STRUCTURE_LEVEL_INVALID",
                    "A structure node can only belong under a parent from the immediately previous level.");
        }
    }

    public static void assertArtifactAllowed(ProjectStructureLevelDefinition level, ProjectStructureArtifactType artifactType) {
        boolean allowed = switch (artifactType) {
            case MILESTONE -> level.allowsMilestones();
            case DELIVERABLE -> level.allowsDeliverables();
        };
        if (!allowed) {
            throw new BusinessRuleException(
                    switch (artifactType) {
                        case MILESTONE -> "PROJECT_STRUCTURE_LEVEL_MILESTONE_NOT_ALLOWED";
                        case DELIVERABLE -> "PROJECT_STRUCTURE_LEVEL_DELIVERABLE_NOT_ALLOWED";
                    },
                    switch (artifactType) {
                        case MILESTONE -> "The selected structure level does not allow milestones.";
                        case DELIVERABLE -> "The selected structure level does not allow deliverables.";
                    },
                    Map.of("levelId", level.id(), "sequence", level.sequence(), "artifactType", artifactType.name()));
        }
    }

    public static ProjectVisibilityScope inheritedVisibility(ProjectVisibilityScope inherited, ProjectVisibilityScope override) {
        return override != null ? override : inherited;
    }
}
