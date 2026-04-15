package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectStructureRulesTest {

    @Test
    void shouldRejectIntermediateLevelWithoutChildren() {
        List<ProjectStructureLevelDefinition> levels = List.of(
                new ProjectStructureLevelDefinition("L1", 1, false, true, true),
                new ProjectStructureLevelDefinition("L2", 2, false, true, true));

        assertThatThrownBy(() -> ProjectStructureRules.validateTemplate(levels))
                .hasMessageContaining("intermediate project structure level")
                .hasFieldOrPropertyWithValue("code", "PROJECT_STRUCTURE_INTERMEDIATE_LEVEL_MUST_ALLOW_CHILDREN");
    }

    @Test
    void shouldRejectLastLevelAllowingChildren() {
        List<ProjectStructureLevelDefinition> levels = List.of(
                new ProjectStructureLevelDefinition("L1", 1, true, true, true),
                new ProjectStructureLevelDefinition("L2", 2, true, true, true));

        assertThatThrownBy(() -> ProjectStructureRules.validateTemplate(levels))
                .hasMessageContaining("last project structure level")
                .hasFieldOrPropertyWithValue("code", "PROJECT_STRUCTURE_LAST_LEVEL_CANNOT_ALLOW_CHILDREN");
    }

    @Test
    void shouldResolveChildLevelAndArtifactCapabilities() {
        List<ProjectStructureLevelDefinition> levels = List.of(
                new ProjectStructureLevelDefinition("ROOT", 1, true, true, true),
                new ProjectStructureLevelDefinition("VEHICLE", 2, true, true, false),
                new ProjectStructureLevelDefinition("PART", 3, false, false, true));

        ProjectStructureLevelDefinition child = ProjectStructureRules.childLevelOf(levels.getFirst(), levels);

        assertThatCode(() -> ProjectStructureRules.assertArtifactAllowed(child, ProjectStructureArtifactType.MILESTONE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProjectStructureRules.assertArtifactAllowed(child, ProjectStructureArtifactType.DELIVERABLE))
                .hasFieldOrPropertyWithValue("code", "PROJECT_STRUCTURE_LEVEL_DELIVERABLE_NOT_ALLOWED");
        assertThatCode(() -> ProjectStructureRules.assertArtifactAllowed(levels.get(2), ProjectStructureArtifactType.DELIVERABLE)).doesNotThrowAnyException();
    }
}
