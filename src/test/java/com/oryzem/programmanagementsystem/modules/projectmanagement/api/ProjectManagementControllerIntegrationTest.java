package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentBindingJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentChecksumSupport;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.InMemoryDocumentStorageStub;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataDeliverableSubmissionDocumentJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataDeliverableSubmissionJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectDeliverableJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectIdempotencyJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectMemberJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectMilestoneJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectOrganizationJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectPurgeIntentJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectPhaseJpaRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.infrastructure.SpringDataProjectStructureNodeJpaRepository;
import com.oryzem.programmanagementsystem.platform.audit.SpringDataAuditLogJpaRepository;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
@Import(ProjectManagementControllerIntegrationTest.ProjectManagementTestConfiguration.class)
class ProjectManagementControllerIntegrationTest {

    @TestConfiguration
    static class ProjectManagementTestConfiguration {
        @Bean
        @Primary
        InMemoryDocumentStorageStub documentStorage() {
            return new InMemoryDocumentStorageStub();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private SpringDataProjectIdempotencyJpaRepository idempotencyRepository;

    @Autowired
    private SpringDataDeliverableSubmissionDocumentJpaRepository submissionDocumentRepository;

    @Autowired
    private SpringDataDeliverableSubmissionJpaRepository submissionRepository;

    @Autowired
    private SpringDataProjectDeliverableJpaRepository deliverableRepository;

    @Autowired
    private SpringDataProjectMilestoneJpaRepository milestoneRepository;

    @Autowired
    private SpringDataProjectPhaseJpaRepository phaseRepository;

    @Autowired
    private SpringDataProjectMemberJpaRepository memberRepository;

    @Autowired
    private SpringDataProjectOrganizationJpaRepository organizationRepository;

    @Autowired
    private SpringDataProjectJpaRepository projectRepository;

    @Autowired
    private SpringDataProjectStructureNodeJpaRepository structureNodeRepository;

    @Autowired
    private SpringDataProjectPurgeIntentJpaRepository purgeIntentRepository;

    @Autowired
    private SpringDataDocumentBindingJpaRepository documentBindingRepository;

    @Autowired
    private SpringDataDocumentJpaRepository documentRepository;

    @Autowired
    private SpringDataAuditLogJpaRepository auditLogRepository;

    @Autowired
    private DocumentStorage documentStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stubStorage().clear();
        documentBindingRepository.deleteAll();
        documentRepository.deleteAll();
        submissionDocumentRepository.deleteAll();
        submissionRepository.deleteAll();
        deliverableRepository.deleteAll();
        milestoneRepository.deleteAll();
        phaseRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        projectRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM deliverable_template WHERE id IN ('DT-CUS-002')");
        jdbcTemplate.update("DELETE FROM project_milestone_template WHERE id IN ('PMT-CUS-002')");
        jdbcTemplate.update("DELETE FROM project_structure_level_template WHERE id IN ('PSLT-CUS-002', 'PSLT-CUS-003')");
        jdbcTemplate.update(
                "UPDATE project_structure_level_template SET sequence_no = 1, name = 'Project', code = 'PROJECT', allows_children = FALSE, allows_milestones = TRUE, allows_deliverables = TRUE WHERE id = 'PSLT-CUS-001'");
        jdbcTemplate.update("DELETE FROM project_template WHERE id NOT IN ('TMP-APQP-V1', 'TMP-VDA-MLA-V1', 'TMP-CUSTOM-V1')");
        jdbcTemplate.update("DELETE FROM project_structure_template WHERE id NOT IN ('PST-APQP-V1', 'PST-VDA-MLA-V1', 'PST-CUSTOM-V1')");
        jdbcTemplate.update("DELETE FROM project_framework WHERE id NOT IN ('PFR-APQP', 'PFR-VDA-MLA', 'PFR-CUSTOM')");
        jdbcTemplate.update("UPDATE project_framework SET display_name = 'APQP', description = 'Sequential product quality planning framework.', ui_layout = 'TIMELINE', active = TRUE WHERE id = 'PFR-APQP'");
        jdbcTemplate.update("UPDATE project_framework SET display_name = 'VDA MLA', description = 'Sequential maturity level assurance framework.', ui_layout = 'TIMELINE', active = TRUE WHERE id = 'PFR-VDA-MLA'");
        jdbcTemplate.update("UPDATE project_framework SET display_name = 'Custom', description = 'Flexible framework for tenant-defined project delivery flows.', ui_layout = 'HYBRID', active = TRUE WHERE id = 'PFR-CUSTOM'");
        idempotencyRepository.deleteAll();
        purgeIntentRepository.deleteAll();
        auditLogRepository.deleteAll();
        bootstrapDataService.reset();
    }

    @Test
    void shouldCreateProjectInstantiateTemplateAndManageParticipants() throws Exception {
        String supplierOrganizationId = createSupplierOrganization();
        JsonNode project = createProject("PRJ-APQP-001");
        String projectId = project.get("id").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/organizations")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organizationId", supplierOrganizationId,
                                "roleType", "SUPPLIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(supplierOrganizationId));

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "USR-EXT-A-MGR-001",
                                "organizationId", "tenant-a",
                                "projectRole", "COORDINATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("USR-EXT-A-MGR-001"));

        mockMvc.perform(get("/api/projects/" + projectId + "/milestones")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/projects/" + projectId + "/deliverables")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        assertThat(structureNodeRepository.findAllByProjectIdOrderBySequenceNoAscIdAsc(projectId))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.getId()).isEqualTo(projectId + "-ROOT");
                    assertThat(node.getCode()).isEqualTo("PRJ-APQP-001");
                });
        assertThat(milestoneRepository.findAllByProjectIdOrderBySequenceNoAsc(projectId))
                .allSatisfy(milestone -> assertThat(milestone.getStructureNodeId()).isEqualTo(projectId + "-ROOT"));
        assertThat(deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId))
                .allSatisfy(deliverable -> assertThat(deliverable.getStructureNodeId()).isEqualTo(projectId + "-ROOT"));
    }

    @Test
    void shouldSupportDeliverableSubmissionAndRealDocumentHosts() throws Exception {
        JsonNode project = createProject("PRJ-FLOW-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);
        String deliverableId = deliverable.get("id").asText();
        long deliverableVersion = deliverable.get("version").asLong();

        String submissionResponse = mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverableVersion,
                                "documentIds", java.util.List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentIds.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode submission = objectMapper.readTree(submissionResponse);
        String submissionId = submission.get("id").asText();

        String submissionDocumentId = initiateAndFinalizeDocument("PROJECT_DELIVERABLE_SUBMISSION", submissionId);
        assertThat(submissionDocumentId).isNotBlank();

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions/" + submissionId + "/approve")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "approve-" + submissionId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reviewComment", "Looks good",
                                "version", submission.get("version").asLong()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/projects/" + projectId + "/dashboard")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedCount").value(1));
    }

    @Test
    void shouldFailSubmissionWhenDocumentDoesNotExist() throws Exception {
        JsonNode project = createProject("PRJ-DOC-MISS-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverable.get("id").asText() + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-missing-doc-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverable.get("version").asLong(),
                                "documentIds", java.util.List.of("DOC-DOES-NOT-EXIST")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailSubmissionWhenDocumentIsNotActive() throws Exception {
        JsonNode project = createProject("PRJ-DOC-PENDING-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);
        String deliverableId = deliverable.get("id").asText();

        String initiateResponse = mockMvc.perform(post("/api/document-contexts/PROJECT_DELIVERABLE/" + deliverableId + "/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("pending.pdf")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pendingDocumentId = objectMapper.readTree(initiateResponse).get("documentId").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-pending-doc-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverable.get("version").asLong(),
                                "documentIds", java.util.List.of(pendingDocumentId)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_ACTIVE"));
    }

    @Test
    void shouldFailSubmissionWhenDocumentContextIsWrong() throws Exception {
        JsonNode project = createProject("PRJ-DOC-CONTEXT-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);
        String wrongContextDocumentId = initiateAndFinalizeDocument("PROJECT", projectId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverable.get("id").asText() + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-wrong-context-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverable.get("version").asLong(),
                                "documentIds", java.util.List.of(wrongContextDocumentId)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_CONTEXT_MISMATCH"));
    }

    @Test
    void shouldFailSubmissionWhenDocumentUsesDeliverableWorkContext() throws Exception {
        JsonNode project = createProject("PRJ-DOC-WORK-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);
        String deliverableId = deliverable.get("id").asText();
        String workDocumentId = initiateAndFinalizeDocument("PROJECT_DELIVERABLE", deliverableId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-work-doc-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverable.get("version").asLong(),
                                "documentIds", java.util.List.of(workDocumentId)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_CONTEXT_MISMATCH"));
    }

    @Test
    void shouldReturnNotFoundWhenApprovingSubmissionThatDoesNotExist() throws Exception {
        JsonNode project = createProject("PRJ-SUB-NF-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverable.get("id").asText() + "/submissions/SUB-404/approve")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "approve-missing-sub-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reviewComment", "Looks good",
                                "version", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateStructureNodeAndInstantiateLevelArtifacts() throws Exception {
        enableCustomTwoLevelStructure();

        JsonNode project = createProject("PRJ-CUSTOM-001", "CUSTOM");
        String projectId = project.get("id").asText();
        String rootNodeId = projectId + "-ROOT";

        String createNodeResponse = mockMvc.perform(post("/api/projects/" + projectId + "/structure/nodes")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "parentNodeId", rootNodeId,
                                "name", "Subsystem A",
                                "code", "SUBSYS-A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentNodeId").value(rootNodeId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String childNodeId = objectMapper.readTree(createNodeResponse).get("id").asText();

        mockMvc.perform(get("/api/projects/" + projectId + "/structure")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels.length()").value(2))
                .andExpect(jsonPath("$.nodes.length()").value(2));

        mockMvc.perform(get("/api/projects/" + projectId + "/milestones")
                        .with(externalAdminTenantA())
                        .param("structureNodeId", childNodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].structureNodeId").value(childNodeId))
                .andExpect(jsonPath("$[0].code").value("CUSTOM_SUBSYSTEM_GATE"));

        mockMvc.perform(get("/api/projects/" + projectId + "/deliverables")
                        .with(externalAdminTenantA())
                        .param("structureNodeId", childNodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].structureNodeId").value(childNodeId))
                .andExpect(jsonPath("$[0].code").value("CUSTOM_SUBSYSTEM_PACKAGE"));
    }

    @Test
    void shouldUpdateStructureNode() throws Exception {
        enableCustomTwoLevelStructure();

        JsonNode project = createProject("PRJ-CUSTOM-UPD-001", "CUSTOM");
        String projectId = project.get("id").asText();
        JsonNode node = createStructureNode(projectId, projectId + "-ROOT", "Subsystem A", "SUBSYS-A");

        mockMvc.perform(patch("/api/projects/" + projectId + "/structure/nodes/" + node.get("id").asText())
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Subsystem Alpha",
                                "code", "SUBSYS-ALPHA",
                                "responsibleUserId", "USR-EXT-A-MGR-001",
                                "visibilityScope", "RESPONSIBLE_AND_APPROVER",
                                "version", node.get("version").asLong()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Subsystem Alpha"))
                .andExpect(jsonPath("$.code").value("SUBSYS-ALPHA"))
                .andExpect(jsonPath("$.responsibleUserId").value("USR-EXT-A-MGR-001"))
                .andExpect(jsonPath("$.visibilityScope").value("RESPONSIBLE_AND_APPROVER"));

        mockMvc.perform(get("/api/projects/" + projectId + "/structure")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[1].name").value("Subsystem Alpha"))
                .andExpect(jsonPath("$.nodes[1].code").value("SUBSYS-ALPHA"));
    }

    @Test
    void shouldFilterMilestonesAndDashboardByActorVisibility() throws Exception {
        String supplierOrganizationId = createSupplierOrganization();
        JsonNode supplierUser = createExternalUser("Supplier Viewer", "supplier.viewer@tenant-a.com", supplierOrganizationId, "ADMIN");
        JsonNode project = createProject("PRJ-VIS-MATRIX-001");
        String projectId = project.get("id").asText();

        addProjectOrganization(projectId, supplierOrganizationId, "SUPPLIER");

        JsonNode milestones = listMilestonesAs(externalAdminTenantA(), projectId, null);
        updateMilestone(projectId, milestones.get(0), "AT_RISK", null, "LEAD_ONLY");
        updateMilestone(projectId, milestones.get(1), "AT_RISK", supplierOrganizationId, "RESPONSIBLE_AND_APPROVER");

        JsonNode deliverables = listDeliverablesAs(externalAdminTenantA(), projectId, null);
        updateDeliverableVisibility(projectId, deliverables.get(0), "LEAD_ONLY");
        updateDeliverableVisibility(projectId, deliverables.get(1), "ALL_PROJECT_PARTICIPANTS");
        updateDeliverableVisibility(projectId, deliverables.get(2), "ALL_PROJECT_PARTICIPANTS");

        mockMvc.perform(get("/api/projects/" + projectId + "/milestones")
                        .with(jwtFor("supplier.viewer@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.code == '" + milestones.get(0).get("code").asText() + "')]").isEmpty())
                .andExpect(jsonPath("$[?(@.code == '" + milestones.get(1).get("code").asText() + "')]").isNotEmpty());

        mockMvc.perform(get("/api/projects/" + projectId + "/dashboard")
                        .with(jwtFor("supplier.viewer@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeliverables").value(2))
                .andExpect(jsonPath("$.milestonesAtRisk").value(1));

        mockMvc.perform(get("/api/projects/" + projectId + "/dashboard")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeliverables").value(3))
                .andExpect(jsonPath("$.milestonesAtRisk").value(2));

        assertThat(supplierUser.get("id").asText()).isNotBlank();
    }

    @Test
    void shouldHideInvisibleSubtreeAndDenyScopedReadsForHiddenNode() throws Exception {
        enableCustomThreeLevelStructure();

        String supplierOrganizationId = createSupplierOrganization();
        JsonNode responsibleUser = createExternalUser("Supplier Responsible", "supplier.responsible@tenant-a.com", supplierOrganizationId, "ADMIN");
        createExternalUser("Supplier Observer", "supplier.observer@tenant-a.com", supplierOrganizationId, "MEMBER");

        JsonNode project = createProject("PRJ-STRUCT-VIS-001", "CUSTOM");
        String projectId = project.get("id").asText();
        addProjectOrganization(projectId, supplierOrganizationId, "SUPPLIER");

        String rootNodeId = projectId + "-ROOT";
        JsonNode subsystem = createStructureNode(projectId, rootNodeId, "Subsystem A", "SUBSYS-A");
        JsonNode component = createStructureNode(projectId, subsystem.get("id").asText(), "Component 1", "COMP-1");

        mockMvc.perform(patch("/api/projects/" + projectId + "/structure/nodes/" + subsystem.get("id").asText())
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", subsystem.get("name").asText(),
                                "code", subsystem.get("code").asText(),
                                "ownerOrganizationId", "tenant-a",
                                "responsibleUserId", responsibleUser.get("id").asText(),
                                "visibilityScope", "RESPONSIBLE_AND_APPROVER",
                                "version", subsystem.get("version").asLong()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projectId + "/structure")
                        .with(jwtFor("supplier.observer@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$..id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(subsystem.get("id").asText()))))
                .andExpect(jsonPath("$..id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(component.get("id").asText()))));

        mockMvc.perform(get("/api/projects/" + projectId + "/structure")
                        .with(jwtFor("supplier.responsible@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$..id").value(org.hamcrest.Matchers.hasItem(subsystem.get("id").asText())))
                .andExpect(jsonPath("$..id").value(org.hamcrest.Matchers.hasItem(component.get("id").asText())));

        mockMvc.perform(get("/api/projects/" + projectId + "/milestones")
                        .param("structureNodeId", subsystem.get("id").asText())
                        .with(jwtFor("supplier.observer@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/" + projectId + "/dashboard")
                        .param("structureNodeId", subsystem.get("id").asText())
                        .with(jwtFor("supplier.observer@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyHiddenDeliverableAndSubmissionDetailsForNonPrivilegedActor() throws Exception {
        String supplierOrganizationId = createSupplierOrganization();
        createExternalUser("Supplier Viewer", "supplier.viewer.detail@tenant-a.com", supplierOrganizationId, "ADMIN");

        JsonNode project = createProject("PRJ-DETAIL-VIS-001");
        String projectId = project.get("id").asText();
        addProjectOrganization(projectId, supplierOrganizationId, "SUPPLIER");

        JsonNode deliverables = listDeliverablesAs(externalAdminTenantA(), projectId, null);
        JsonNode hiddenDeliverable = deliverables.get(0);
        updateDeliverableVisibility(projectId, hiddenDeliverable, "LEAD_ONLY");
        JsonNode hiddenDeliverableDetail = getDeliverableAs(externalAdminTenantA(), projectId, hiddenDeliverable.get("id").asText());

        String submissionResponse = mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + hiddenDeliverable.get("id").asText() + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "submit-hidden-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", hiddenDeliverableDetail.get("version").asLong(),
                                "documentIds", java.util.List.of()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String submissionId = objectMapper.readTree(submissionResponse).get("id").asText();

        mockMvc.perform(get("/api/projects/" + projectId + "/deliverables/" + hiddenDeliverable.get("id").asText())
                        .with(jwtFor("supplier.viewer.detail@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/" + projectId + "/deliverables/" + hiddenDeliverable.get("id").asText() + "/submissions/" + submissionId)
                        .with(jwtFor("supplier.viewer.detail@tenant-a.com", "ROLE_MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldMoveStructureNodeBetweenParents() throws Exception {
        enableCustomThreeLevelStructure();

        JsonNode project = createProject("PRJ-CUSTOM-MOVE-001", "CUSTOM");
        String projectId = project.get("id").asText();
        String rootNodeId = projectId + "-ROOT";
        JsonNode subsystemA = createStructureNode(projectId, rootNodeId, "Subsystem A", "SUBSYS-A");
        JsonNode subsystemB = createStructureNode(projectId, rootNodeId, "Subsystem B", "SUBSYS-B");
        JsonNode component = createStructureNode(projectId, subsystemA.get("id").asText(), "Component 1", "COMP-1");

        mockMvc.perform(post("/api/projects/" + projectId + "/structure/nodes/" + component.get("id").asText() + "/move")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newParentNodeId", subsystemB.get("id").asText(),
                                "version", component.get("version").asLong()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentNodeId").value(subsystemB.get("id").asText()));

        assertThat(structureNodeRepository.findByIdAndProjectId(component.get("id").asText(), projectId))
                .get()
                .satisfies(node -> assertThat(node.getParentNodeId()).isEqualTo(subsystemB.get("id").asText()));
    }

    @Test
    void shouldInheritParentVisibilityWhenCreatingStructureNode() throws Exception {
        enableCustomTwoLevelStructure();
        String response = mockMvc.perform(post("/api/projects")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "PRJ-CUSTOM-VIS-001")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PRJ-CUSTOM-VIS-001",
                                "name", "Project PRJ-CUSTOM-VIS-001",
                                "description", "visibility inheritance",
                                "frameworkType", "CUSTOM",
                                "visibilityScope", "LEAD_ONLY",
                                "plannedStartDate", "2026-04-08",
                                "plannedEndDate", "2026-06-30"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String projectId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/structure/nodes")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "parentNodeId", projectId + "-ROOT",
                                "name", "Subsystem A",
                                "code", "SUBSYS-A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilityScope").value("LEAD_ONLY"));
    }

    @Test
    void shouldRejectInvalidStructureTemplateWhenIntermediateLevelDisablesChildren() throws Exception {
        enableCustomTwoLevelStructure();

        mockMvc.perform(patch("/api/project-structure-templates/PST-CUSTOM-V1/levels/PSLT-CUS-001")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Program",
                                "code", "PROGRAM",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListResponsibleDeliverablesForCurrentActor() throws Exception {
        JsonNode project = createProject("PRJ-RESP-001");
        String projectId = project.get("id").asText();

        mockMvc.perform(get("/api/projects/" + projectId + "/deliverables/responsible")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldListAndDetailStructureTemplates() throws Exception {
        mockMvc.perform(get("/api/project-structure-templates")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").exists());

        mockMvc.perform(get("/api/project-structure-templates/PST-APQP-V1")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PST-APQP-V1"))
                .andExpect(jsonPath("$.levels.length()").value(1))
                .andExpect(jsonPath("$.projectTemplates.length()").value(1))
                .andExpect(jsonPath("$.milestoneTemplates.length()").value(3))
                .andExpect(jsonPath("$.deliverableTemplates.length()").value(3));
    }

    @Test
    void shouldCreateManageAndActivateStructureTemplate() throws Exception {
        String createResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vehicle Breakdown v2",
                                "frameworkType", "CUSTOM",
                                "version", 2,
                                "active", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vehicle Breakdown v2"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.levels.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(createResponse).get("id").asText();

        String level1Response = mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Program",
                                "code", "PROGRAM",
                                "allowsChildren", true,
                                "allowsMilestones", false,
                                "allowsDeliverables", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String level1Id = objectMapper.readTree(level1Response).get("id").asText();

        String level2Response = mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vehicle",
                                "code", "VEHICLE",
                                "allowsChildren", true,
                                "allowsMilestones", true,
                                "allowsDeliverables", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String level2Id = objectMapper.readTree(level2Response).get("id").asText();

        String level3Response = mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Component",
                                "code", "COMPONENT",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String level3Id = objectMapper.readTree(level3Response).get("id").asText();

        mockMvc.perform(patch("/api/project-structure-templates/" + structureTemplateId)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Vehicle Breakdown v2.1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vehicle Breakdown v2.1"));

        mockMvc.perform(patch("/api/project-structure-templates/" + structureTemplateId + "/levels/" + level3Id)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Assembly",
                                "code", "ASSEMBLY",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Assembly"))
                .andExpect(jsonPath("$.code").value("ASSEMBLY"));

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels/reorder")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderedLevelIds", java.util.List.of(level1Id, level3Id, level2Id)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(level1Id))
                .andExpect(jsonPath("$[0].sequence").value(1))
                .andExpect(jsonPath("$[1].id").value(level3Id))
                .andExpect(jsonPath("$[1].sequence").value(2))
                .andExpect(jsonPath("$[2].id").value(level2Id))
                .andExpect(jsonPath("$[2].sequence").value(3));

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/activate")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/deactivate")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/project-structure-templates/" + structureTemplateId)
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vehicle Breakdown v2.1"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.levels.length()").value(3))
                .andExpect(jsonPath("$.levels[1].id").value(level3Id))
                .andExpect(jsonPath("$.levels[1].name").value("Assembly"))
                .andExpect(jsonPath("$.projectTemplates.length()").value(0));

        mockMvc.perform(get("/api/project-structure-templates")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void shouldCreateAndUpdateProjectTemplateLinkedToStructureTemplate() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Customer Program v3",
                                "frameworkType", "CUSTOM",
                                "version", 3,
                                "active", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Project",
                                "code", "PROJECT",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk());

        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Custom Program Template v3",
                                "frameworkType", "CUSTOM",
                                "version", 3,
                                "status", "ACTIVE",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frameworkType").value("CUSTOM"))
                .andExpect(jsonPath("$.structureTemplateId").value(structureTemplateId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).get("id").asText();

        mockMvc.perform(get("/api/project-templates")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/api/project-templates/" + templateId)
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Custom Program Template v3"))
                .andExpect(jsonPath("$.structureTemplateId").value(structureTemplateId));

        mockMvc.perform(patch("/api/project-templates/" + templateId)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Custom Program Template v3.1",
                                "status", "RETIRED",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Custom Program Template v3.1"))
                .andExpect(jsonPath("$.status").value("RETIRED"))
                .andExpect(jsonPath("$.structureTemplateId").value(structureTemplateId));
    }

    @Test
    void shouldManageProjectTemplatePhasesMilestonesAndDeliverables() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Execution Breakdown v4",
                                "frameworkType", "CUSTOM",
                                "version", 4,
                                "active", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        String rootLevelResponse = mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Project",
                                "code", "PROJECT",
                                "allowsChildren", true,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rootLevelId = objectMapper.readTree(rootLevelResponse).get("id").asText();

        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Execution Template v4",
                                "frameworkType", "CUSTOM",
                                "version", 4,
                                "status", "ACTIVE",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).get("id").asText();

        String phaseResponse = mockMvc.perform(post("/api/project-templates/" + templateId + "/phases")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Execution",
                                "description", "Execution phase",
                                "plannedStartOffsetDays", 0,
                                "plannedEndOffsetDays", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String phaseId = objectMapper.readTree(phaseResponse).get("id").asText();

        mockMvc.perform(get("/api/project-templates/" + templateId + "/phases")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(phaseId));

        mockMvc.perform(patch("/api/project-templates/" + templateId + "/phases/" + phaseId)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Execution Updated",
                                "description", "Execution phase updated",
                                "plannedStartOffsetDays", 1,
                                "plannedEndOffsetDays", 35))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Execution Updated"));

        String milestoneResponse = mockMvc.perform(post("/api/project-templates/" + templateId + "/milestones")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phaseTemplateId", phaseId,
                                "code", "EXEC_GATE",
                                "name", "Execution Gate",
                                "description", "Execution approval gate",
                                "plannedOffsetDays", 20,
                                "appliesToType", "ROOT_NODE",
                                "structureLevelTemplateId", rootLevelId,
                                "ownerOrganizationRole", "LEAD",
                                "visibilityScope", "ALL_PROJECT_PARTICIPANTS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXEC_GATE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String milestoneId = objectMapper.readTree(milestoneResponse).get("id").asText();

        mockMvc.perform(get("/api/project-templates/" + templateId + "/milestones")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(milestoneId));

        mockMvc.perform(patch("/api/project-templates/" + templateId + "/milestones/" + milestoneId)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phaseTemplateId", phaseId,
                                "code", "EXEC_GATE_1",
                                "name", "Execution Gate Updated",
                                "description", "Execution approval gate updated",
                                "plannedOffsetDays", 22,
                                "appliesToType", "ROOT_NODE",
                                "structureLevelTemplateId", rootLevelId,
                                "ownerOrganizationRole", "LEAD",
                                "visibilityScope", "RESPONSIBLE_AND_APPROVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXEC_GATE_1"))
                .andExpect(jsonPath("$.visibilityScope").value("RESPONSIBLE_AND_APPROVER"));

        String deliverableResponse = mockMvc.perform(post("/api/project-templates/" + templateId + "/deliverables")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.ofEntries(
                                java.util.Map.entry("phaseTemplateId", phaseId),
                                java.util.Map.entry("milestoneTemplateId", milestoneId),
                                java.util.Map.entry("code", "EXEC_PACKAGE"),
                                java.util.Map.entry("name", "Execution Package"),
                                java.util.Map.entry("description", "Execution package"),
                                java.util.Map.entry("deliverableType", "DOCUMENT_PACKAGE"),
                                java.util.Map.entry("requiredDocument", true),
                                java.util.Map.entry("plannedDueOffsetDays", 25),
                                java.util.Map.entry("appliesToType", "ROOT_NODE"),
                                java.util.Map.entry("structureLevelTemplateId", rootLevelId),
                                java.util.Map.entry("responsibleOrganizationRole", "LEAD"),
                                java.util.Map.entry("approverOrganizationRole", "CUSTOMER"),
                                java.util.Map.entry("visibilityScope", "ALL_PROJECT_PARTICIPANTS"),
                                java.util.Map.entry("priority", "HIGH")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXEC_PACKAGE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String deliverableId = objectMapper.readTree(deliverableResponse).get("id").asText();

        mockMvc.perform(get("/api/project-templates/" + templateId + "/deliverables")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(deliverableId));

        mockMvc.perform(patch("/api/project-templates/" + templateId + "/deliverables/" + deliverableId)
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.ofEntries(
                                java.util.Map.entry("phaseTemplateId", phaseId),
                                java.util.Map.entry("milestoneTemplateId", milestoneId),
                                java.util.Map.entry("code", "EXEC_PACKAGE_V2"),
                                java.util.Map.entry("name", "Execution Package Updated"),
                                java.util.Map.entry("description", "Execution package updated"),
                                java.util.Map.entry("deliverableType", "EVIDENCE_PACKAGE"),
                                java.util.Map.entry("requiredDocument", true),
                                java.util.Map.entry("plannedDueOffsetDays", 28),
                                java.util.Map.entry("appliesToType", "ROOT_NODE"),
                                java.util.Map.entry("structureLevelTemplateId", rootLevelId),
                                java.util.Map.entry("responsibleOrganizationRole", "SUPPLIER"),
                                java.util.Map.entry("approverOrganizationRole", "CUSTOMER"),
                                java.util.Map.entry("visibilityScope", "RESPONSIBLE_AND_APPROVER"),
                                java.util.Map.entry("priority", "CRITICAL")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXEC_PACKAGE_V2"))
                .andExpect(jsonPath("$.deliverableType").value("EVIDENCE_PACKAGE"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void shouldPurgeUnusedProjectTemplate() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Purgeable Structure",
                                "frameworkType", "CUSTOM",
                                "version", 7,
                                "active", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Project",
                                "code", "PROJECT",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk());

        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Purgeable Template",
                                "frameworkType", "CUSTOM",
                                "version", 7,
                                "status", "RETIRED",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-templates/" + templateId + "/purge")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId))
                .andExpect(jsonPath("$.name").value("Purgeable Template"));

        mockMvc.perform(get("/api/project-templates/" + templateId)
                        .with(externalAdminTenantA()))
                .andExpect(status().isNotFound());

        assertThat(auditLogRepository.findAll()).anySatisfy(event -> {
            assertThat(event.toDomain().eventType()).isEqualTo("PROJECT_TEMPLATE_PURGED");
            assertThat(event.toDomain().targetResourceType()).isEqualTo("PROJECT_TEMPLATE");
            assertThat(event.toDomain().targetResourceId()).isEqualTo(templateId);
            assertThat(event.toDomain().metadataJson()).contains("Purgeable Template");
        });
    }

    @Test
    void shouldRejectProjectTemplatePurgeWhenTemplateIsDefault() throws Exception {
        mockMvc.perform(post("/api/project-templates/TMP-CUSTOM-V1/purge")
                        .with(internalAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_TEMPLATE_DEFAULT_CANNOT_BE_PURGED"));
    }

    @Test
    void shouldRejectProjectTemplatePurgeWhenTemplateIsUsedByProject() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Used Structure",
                                "frameworkType", "CUSTOM",
                                "version", 8,
                                "active", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/levels")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Project",
                                "code", "PROJECT",
                                "allowsChildren", false,
                                "allowsMilestones", true,
                                "allowsDeliverables", true))))
                .andExpect(status().isOk());

        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Used Template",
                                "frameworkType", "CUSTOM",
                                "version", 8,
                                "status", "ACTIVE",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).get("id").asText();

        createProject("PRJ-CUS-USED-001", "CUSTOM", templateId);

        mockMvc.perform(post("/api/project-templates/" + templateId + "/purge")
                        .with(externalAdminTenantA()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_TEMPLATE_IN_USE"));
    }

    @Test
    void shouldPurgeUnusedProjectStructureTemplate() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Disposable Structure",
                                "frameworkType", "CUSTOM",
                                "version", 9,
                                "active", false))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/purge")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(structureTemplateId))
                .andExpect(jsonPath("$.name").value("Disposable Structure"));

        mockMvc.perform(get("/api/project-structure-templates/" + structureTemplateId)
                        .with(externalAdminTenantA()))
                .andExpect(status().isNotFound());

        assertThat(auditLogRepository.findAll()).anySatisfy(event -> {
            assertThat(event.toDomain().eventType()).isEqualTo("PROJECT_STRUCTURE_TEMPLATE_PURGED");
            assertThat(event.toDomain().targetResourceType()).isEqualTo("PROJECT_STRUCTURE_TEMPLATE");
            assertThat(event.toDomain().targetResourceId()).isEqualTo(structureTemplateId);
            assertThat(event.toDomain().metadataJson()).contains("Disposable Structure");
        });
    }

    @Test
    void shouldRejectProjectStructureTemplatePurgeWhenReferencedByProjectTemplate() throws Exception {
        String structureTemplateResponse = mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Referenced Structure",
                                "frameworkType", "CUSTOM",
                                "version", 10,
                                "active", true))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String structureTemplateId = objectMapper.readTree(structureTemplateResponse).get("id").asText();

        mockMvc.perform(post("/api/project-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Referencing Template",
                                "frameworkType", "CUSTOM",
                                "version", 10,
                                "status", "RETIRED",
                                "isDefault", false,
                                "structureTemplateId", structureTemplateId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/project-structure-templates/" + structureTemplateId + "/purge")
                        .with(externalAdminTenantA()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_STRUCTURE_TEMPLATE_IN_USE"));
    }

    @Test
    void shouldAllowSupplierInChainToViewAndUseTemplatesButDenyManagement() throws Exception {
        String supplierOrganizationId = createSupplierOrganization();
        createExternalUser("Supplier Admin Templates", "supplier.templates.admin@tenant-a.com", supplierOrganizationId, "ADMIN");

        mockMvc.perform(get("/api/project-templates")
                        .with(jwtFor("supplier.templates.admin@tenant-a.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(patch("/api/project-templates/TMP-CUSTOM-V1")
                        .with(jwtFor("supplier.templates.admin@tenant-a.com", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Supplier Attempt",
                                "status", "ACTIVE",
                                "isDefault", true,
                                "structureTemplateId", "PST-CUSTOM-V1"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/project-templates/TMP-CUSTOM-V1/purge")
                        .with(jwtFor("supplier.templates.admin@tenant-a.com", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/projects")
                        .with(jwtFor("supplier.templates.admin@tenant-a.com", "ROLE_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "PRJ-SUP-TEMPLATE-USE-001")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PRJ-SUP-TEMPLATE-USE-001",
                                "name", "Project Supplier Template Use",
                                "description", "supplier uses inherited template",
                                "frameworkType", "CUSTOM",
                                "templateId", "TMP-CUSTOM-V1",
                                "plannedStartDate", "2026-04-08",
                                "plannedEndDate", "2026-06-30"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(response).get("templateId").asText()).isEqualTo("TMP-CUSTOM-V1");
    }

    @Test
    void shouldExposePlatformDefaultTemplatesToOtherTenantsButKeepManagementForInternalAdmin() throws Exception {
        mockMvc.perform(get("/api/project-templates")
                        .with(externalAdminTenantB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/api/projects")
                        .with(externalAdminTenantB())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "PRJ-TENANT-B-TEMPLATE-001")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PRJ-TENANT-B-TEMPLATE-001",
                                "name", "Project Tenant B",
                                "description", "cross-chain template use",
                                "frameworkType", "CUSTOM",
                                "templateId", "TMP-CUSTOM-V1",
                                "plannedStartDate", "2026-04-08",
                                "plannedEndDate", "2026-06-30"))))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/project-templates/TMP-CUSTOM-V1")
                        .with(externalAdminTenantB())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Tenant B Attempt",
                                "status", "ACTIVE",
                                "isDefault", true,
                                "structureTemplateId", "PST-CUSTOM-V1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowInternalAdminToManagePlatformDefaultTemplates() throws Exception {
        mockMvc.perform(get("/api/project-templates")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/project-structure-templates")
                        .with(internalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(patch("/api/project-templates/TMP-CUSTOM-V1")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Custom Default v1 Managed Internally",
                                "status", "ACTIVE",
                                "isDefault", true,
                                "structureTemplateId", "PST-CUSTOM-V1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Custom Default v1 Managed Internally"));
    }

    @Test
    void shouldListCreateAndUpdateProjectFrameworkCatalog() throws Exception {
        mockMvc.perform(get("/api/project-frameworks")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("APQP"));

        String createResponse = mockMvc.perform(post("/api/project-frameworks")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "KANBAN",
                                "displayName", "Kanban",
                                "description", "Visual flow management",
                                "uiLayout", "BOARD",
                                "active", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KANBAN"))
                .andExpect(jsonPath("$.displayName").value("Kanban"))
                .andExpect(jsonPath("$.uiLayout").value("BOARD"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String frameworkId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(patch("/api/project-frameworks/" + frameworkId)
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Kanban Flow",
                                "description", "Visual workflow with WIP control",
                                "uiLayout", "HYBRID",
                                "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("KANBAN"))
                .andExpect(jsonPath("$.displayName").value("Kanban Flow"))
                .andExpect(jsonPath("$.uiLayout").value("HYBRID"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldRejectProjectFrameworkManagementOutsideInternalAdmin() throws Exception {
        mockMvc.perform(post("/api/project-frameworks")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "KANBAN",
                                "displayName", "Kanban",
                                "description", "Visual flow management",
                                "uiLayout", "BOARD",
                                "active", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInactiveFrameworkWhenCreatingTemplatesOrProjects() throws Exception {
        mockMvc.perform(patch("/api/project-frameworks/PFR-CUSTOM")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Custom",
                                "description", "Flexible framework for tenant-defined project delivery flows.",
                                "uiLayout", "HYBRID",
                                "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/project-structure-templates")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Blocked Custom Structure",
                                "frameworkType", "CUSTOM",
                                "version", 2,
                                "active", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_FRAMEWORK_INACTIVE"));

        mockMvc.perform(post("/api/projects")
                        .with(externalAdminTenantA())
                        .header("Idempotency-Key", "inactive-framework-project")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PRJ-INACTIVE-FW-001",
                                "name", "Inactive Framework Project",
                                "frameworkType", "CUSTOM",
                                "templateId", "TMP-CUSTOM-V1",
                                "plannedStartDate", "2026-04-08",
                                "plannedEndDate", "2026-06-30"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_FRAMEWORK_INACTIVE"));
    }

    @Test
    void shouldBlockCrossTenantDocumentAccessForProjectHost() throws Exception {
        JsonNode project = createProject("PRJ-SEC-001");
        String projectId = project.get("id").asText();

        mockMvc.perform(post("/api/document-contexts/PROJECT/" + projectId + "/documents/uploads")
                        .with(externalAdminTenantB())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("project.pdf")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowInternalAdminToCreateProjectPurgeIntent() throws Exception {
        JsonNode project = createProject("PRJ-PURGE-INTENT-001");
        String projectId = project.get("id").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/purge-intents")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Project is orphaned after tenant offboarding."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.projectCode").value("PRJ-PURGE-INTENT-001"))
                .andExpect(jsonPath("$.requiresFinalConfirmation").value(true))
                .andExpect(jsonPath("$.impact.deliverableCount").value(3));
    }

    @Test
    void shouldAllowInternalSupportToPurgeProjectAndCleanAllArtifacts() throws Exception {
        JsonNode project = createProject("PRJ-PURGE-001");
        String projectId = project.get("id").asText();
        JsonNode deliverable = firstDeliverable(projectId);
        String deliverableId = deliverable.get("id").asText();
        String projectDocumentId = initiateAndFinalizeDocument("PROJECT", projectId);
        String deliverableDocumentId = initiateAndFinalizeDocument("PROJECT_DELIVERABLE", deliverableId);

        String submissionResponse = mockMvc.perform(post("/api/projects/" + projectId + "/deliverables/" + deliverableId + "/submissions")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "purge-submit-" + projectId)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deliverableVersion", deliverable.get("version").asLong(),
                                "documentIds", java.util.List.of()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String submissionId = objectMapper.readTree(submissionResponse).get("id").asText();
        String submissionDocumentId = initiateAndFinalizeDocument("PROJECT_DELIVERABLE_SUBMISSION", submissionId);

        String intentResponse = mockMvc.perform(post("/api/projects/" + projectId + "/purge-intents")
                        .with(internalSupport())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Organization removed from platform; project cleanup required."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impact.documentCount").value(3))
                .andExpect(jsonPath("$.impact.storageObjectCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String purgeToken = objectMapper.readTree(intentResponse).get("purgeToken").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/purge")
                        .with(internalSupport())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Organization removed from platform; project cleanup required.",
                                "purgeToken", purgeToken,
                                "confirm", true,
                                "confirmationText", "PURGE PROJECT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PURGED"))
                .andExpect(jsonPath("$.impact.documentCount").value(3));

        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(deliverableRepository.findAllByProjectIdOrderByPlannedDueDateAscIdAsc(projectId)).isEmpty();
        assertThat(submissionRepository.findById(submissionId)).isEmpty();
        assertThat(documentBindingRepository.findByDocumentId(projectDocumentId)).isEmpty();
        assertThat(documentBindingRepository.findByDocumentId(deliverableDocumentId)).isEmpty();
        assertThat(documentBindingRepository.findByDocumentId(submissionDocumentId)).isEmpty();
        assertThat(documentRepository.findById(projectDocumentId)).isEmpty();
        assertThat(documentRepository.findById(deliverableDocumentId)).isEmpty();
        assertThat(documentRepository.findById(submissionDocumentId)).isEmpty();
        assertThat(stubStorage().listStorageKeys("tenant")).isEmpty();
    }

    @Test
    void shouldRejectProjectPurgeExecutionWithoutFinalConfirmation() throws Exception {
        JsonNode project = createProject("PRJ-PURGE-CONFIRM-001");
        String projectId = project.get("id").asText();

        String intentResponse = mockMvc.perform(post("/api/projects/" + projectId + "/purge-intents")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Validation of destructive safeguards."))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String purgeToken = objectMapper.readTree(intentResponse).get("purgeToken").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/purge")
                        .with(internalAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Validation of destructive safeguards.",
                                "purgeToken", purgeToken,
                                "confirm", true,
                                "confirmationText", "WRONG TEXT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_PURGE_CONFIRMATION_TEXT_INVALID"));

        assertThat(projectRepository.findById(projectId)).isPresent();
    }

    @Test
    void shouldRejectProjectPurgeIntentForExternalActors() throws Exception {
        JsonNode project = createProject("PRJ-PURGE-EXT-001");
        String projectId = project.get("id").asText();

        mockMvc.perform(post("/api/projects/" + projectId + "/purge-intents")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "External actors must not purge projects."))))
                .andExpect(status().isForbidden());
    }

    private JsonNode createProject(String code) throws Exception {
        return createProject(code, "APQP");
    }

    private JsonNode createProject(String code, String frameworkType) throws Exception {
        return createProject(code, frameworkType, null);
    }

    private JsonNode createProject(String code, String frameworkType, String templateId) throws Exception {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("code", code);
        payload.put("name", "Project " + code);
        payload.put("description", "integration flow");
        payload.put("frameworkType", frameworkType);
        if (templateId != null) {
            payload.put("templateId", templateId);
        }
        payload.put("plannedStartDate", "2026-04-08");
        payload.put("plannedEndDate", "2026-06-30");
        String response = mockMvc.perform(post("/api/projects")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", code)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode firstDeliverable(String projectId) throws Exception {
        String response = mockMvc.perform(get("/api/projects/" + projectId + "/deliverables")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get(0);
    }

    private JsonNode listDeliverablesAs(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String projectId,
            String structureNodeId) throws Exception {
        var request = get("/api/projects/" + projectId + "/deliverables").with(actor);
        if (structureNodeId != null) {
            request = request.param("structureNodeId", structureNodeId);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getDeliverableAs(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String projectId,
            String deliverableId) throws Exception {
        String response = mockMvc.perform(get("/api/projects/" + projectId + "/deliverables/" + deliverableId).with(actor))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode listMilestonesAs(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String projectId,
            String structureNodeId) throws Exception {
        var request = get("/api/projects/" + projectId + "/milestones").with(actor);
        if (structureNodeId != null) {
            request = request.param("structureNodeId", structureNodeId);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String createSupplierOrganization() throws Exception {
        String response = mockMvc.perform(post("/api/access/organizations")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Supplier A1",
                                "cnpj", "11111111000191",
                                "localOrganizationCode", "SUP-A1"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private JsonNode createExternalUser(String displayName, String email, String organizationId, String role) throws Exception {
        String response = mockMvc.perform(post("/api/access/users")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", displayName,
                                "email", email,
                                "organizationId", organizationId,
                                "roles", java.util.List.of(role)))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void addProjectOrganization(String projectId, String organizationId, String roleType) throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/organizations")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organizationId", organizationId,
                                "roleType", roleType))))
                .andExpect(status().isOk());
    }

    private void updateMilestone(
            String projectId,
            JsonNode milestone,
            String status,
            String ownerOrganizationId,
            String visibilityScope) throws Exception {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("plannedDate", milestone.get("plannedDate").isNull() ? null : milestone.get("plannedDate").asText());
        payload.put("actualDate", milestone.get("actualDate").isNull() ? null : milestone.get("actualDate").asText());
        payload.put("status", status);
        payload.put("ownerOrganizationId", ownerOrganizationId);
        payload.put("visibilityScope", visibilityScope);
        payload.put("version", milestone.get("version").asLong());
        mockMvc.perform(patch("/api/projects/" + projectId + "/milestones/" + milestone.get("id").asText())
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private void updateDeliverableVisibility(String projectId, JsonNode deliverable, String visibilityScope) throws Exception {
        mockMvc.perform(patch("/api/projects/" + projectId + "/deliverables/" + deliverable.get("id").asText())
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", deliverable.get("description").asText(),
                                "responsibleOrganizationId", deliverable.get("responsibleOrganizationId").asText(),
                                "responsibleUserId", deliverable.get("responsibleUserId").asText(),
                                "approverOrganizationId", deliverable.get("approverOrganizationId").asText(),
                                "approverUserId", deliverable.get("approverUserId").asText(),
                                "plannedDueDate", deliverable.get("plannedDueDate").asText(),
                                "status", deliverable.get("status").asText(),
                                "priority", deliverable.get("priority").asText(),
                                "visibilityScope", visibilityScope,
                                "version", deliverable.get("version").asLong()))))
                .andExpect(status().isOk());
    }

    private JsonNode createStructureNode(String projectId, String parentNodeId, String name, String code) throws Exception {
        String response = mockMvc.perform(post("/api/projects/" + projectId + "/structure/nodes")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "parentNodeId", parentNodeId,
                                "name", name,
                                "code", code))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String initiateAndFinalizeDocument(String contextType, String contextId) throws Exception {
        String initiateResponse = mockMvc.perform(post("/api/document-contexts/" + contextType + "/" + contextId + "/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidence.pdf")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(initiateResponse).get("documentId").asText();
        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        stubStorage().putObject(
                document.getStorageKey(),
                document.getContentType(),
                pdfBytes(),
                Map.of("checksum-sha256", document.getChecksumSha256(), "document-id", document.getId()));
        mockMvc.perform(post("/api/documents/" + documentId + "/finalize-upload")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        return documentId;
    }

    private String initiateRequest(String filename) throws Exception {
        byte[] content = pdfBytes();
        return objectMapper.writeValueAsString(Map.of(
                "originalFilename", filename,
                "contentType", "application/pdf",
                "sizeBytes", content.length,
                "checksumSha256", DocumentChecksumSupport.sha256Hex(content)));
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\noryzem-project\n".getBytes(StandardCharsets.US_ASCII);
    }

    private void enableCustomTwoLevelStructure() {
        jdbcTemplate.update("UPDATE project_structure_level_template SET allows_children = TRUE WHERE id = 'PSLT-CUS-001'");
        jdbcTemplate.update("DELETE FROM deliverable_template WHERE id = 'DT-CUS-002'");
        jdbcTemplate.update("DELETE FROM project_milestone_template WHERE id = 'PMT-CUS-002'");
        jdbcTemplate.update("DELETE FROM project_structure_level_template WHERE id = 'PSLT-CUS-002'");
        jdbcTemplate.update(
                "INSERT INTO project_structure_level_template (id, structure_template_id, sequence_no, name, code, allows_children, allows_milestones, allows_deliverables) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "PSLT-CUS-002",
                "PST-CUSTOM-V1",
                2,
                "Subsystem",
                "SUBSYSTEM",
                false,
                true,
                true);
        jdbcTemplate.update(
                "INSERT INTO project_milestone_template (id, template_id, phase_template_id, code, name, sequence_no, description, planned_offset_days, owner_organization_role, visibility_scope, applies_to_type, structure_level_template_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "PMT-CUS-002",
                "TMP-CUSTOM-V1",
                "PHT-CUS-001",
                "CUSTOM_SUBSYSTEM_GATE",
                "Subsystem Gate",
                2,
                "Subsystem gate.",
                20,
                "LEAD",
                "ALL_PROJECT_PARTICIPANTS",
                "STRUCTURE_LEVEL",
                "PSLT-CUS-002");
        jdbcTemplate.update(
                "INSERT INTO deliverable_template (id, template_id, phase_template_id, milestone_template_id, code, name, description, deliverable_type, required_document, planned_due_offset_days, responsible_organization_role, approver_organization_role, visibility_scope, priority, applies_to_type, structure_level_template_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "DT-CUS-002",
                "TMP-CUSTOM-V1",
                "PHT-CUS-001",
                "PMT-CUS-002",
                "CUSTOM_SUBSYSTEM_PACKAGE",
                "Subsystem Package",
                "Subsystem package.",
                "DOCUMENT_PACKAGE",
                true,
                25,
                "LEAD",
                "CUSTOMER",
                "ALL_PROJECT_PARTICIPANTS",
                "HIGH",
                "STRUCTURE_LEVEL",
                "PSLT-CUS-002");
    }

    private void enableCustomThreeLevelStructure() {
        enableCustomTwoLevelStructure();
        jdbcTemplate.update("UPDATE project_structure_level_template SET allows_children = TRUE WHERE id = 'PSLT-CUS-002'");
        jdbcTemplate.update("DELETE FROM project_structure_level_template WHERE id = 'PSLT-CUS-003'");
        jdbcTemplate.update(
                "INSERT INTO project_structure_level_template (id, structure_template_id, sequence_no, name, code, allows_children, allows_milestones, allows_deliverables) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "PSLT-CUS-003",
                "PST-CUSTOM-V1",
                3,
                "Component",
                "COMPONENT",
                false,
                false,
                false);
    }

    private InMemoryDocumentStorageStub stubStorage() {
        return (InMemoryDocumentStorageStub) documentStorage;
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor externalAdminTenantA() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin.a@tenant.com-sub")
                        .claim("cognito:username", "admin.a@tenant.com")
                        .claim("email", "admin.a@tenant.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalAdmin() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin@oryzem.com-sub")
                        .claim("cognito:username", "admin@oryzem.com")
                        .claim("email", "admin@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor internalSupport() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "support@oryzem.com-sub")
                        .claim("cognito:username", "support@oryzem.com")
                        .claim("email", "support@oryzem.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPPORT"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor externalAdminTenantB() {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", "admin.b@tenant.com-sub")
                        .claim("cognito:username", "admin.b@tenant.com")
                        .claim("email", "admin.b@tenant.com")
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(
            String username,
            String authority) {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", username + "-sub")
                        .claim("cognito:username", username)
                        .claim("email", username)
                        .claim("token_use", "access"))
                .authorities(new SimpleGrantedAuthority(authority));
    }
}
