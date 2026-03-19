package com.oryzem.programmanagementsystem.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.bootstrap.BootstrapDataService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void portfolioEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/portfolio/organizations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldAllowOrganizationCreationOnlyForInternalAdmin() throws Exception {
        mockMvc.perform(post("/api/portfolio/organizations")
                        .with(jwtFor("internal-admin", "internal-core", "INTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Oryzem Internal",
                                  "code": "ORY-INT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ORY-INT"))
                .andExpect(jsonPath("$.setupStatus").value("INCOMPLETED"));

        mockMvc.perform(post("/api/portfolio/organizations")
                        .with(jwtFor("external-admin", "tenant-a", "EXTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Supplier X",
                                  "code": "SUP-X"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only INTERNAL admins can create organizations."));
    }

    @Test
    void shouldRejectProgramCreationWhenOwnerOrganizationIsIncomplete() throws Exception {
        String ownerOrganizationId = createOrganization("Oryzem Internal", "ORY-INT");
        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Program",
                                  "code": "PRG-BLOCK-01",
                                  "description": "Should fail while org is incomplete",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Initial Project",
                                    "code": "PRJ-BLOCK-01",
                                    "description": "Blocked project",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                ownerOrganizationId,
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Owner organization is incomplete and requires an invited or active ADMIN user."));
    }

    @Test
    void shouldCreateAndRetrievePortfolioStructure() throws Exception {
        LocalDate programStartDate = LocalDate.now().plusDays(1);
        LocalDate programEndDate = programStartDate.plusMonths(4);
        LocalDate projectStartDate = programStartDate;
        LocalDate projectEndDate = projectStartDate.plusWeeks(10);
        LocalDate deliverablePlannedDate = projectStartDate.plusWeeks(3);
        LocalDate deliverableDueDate = projectStartDate.plusWeeks(4);

        String ownerOrganizationId = createOrganization("Oryzem Internal", "ORY-INT");
        String supplierOrganizationId = createOrganization("Supplier A", "SUP-A");
        createAdminUser(ownerOrganizationId, "owner.admin@ory-int.com");
        String milestoneTemplateId = createMilestoneTemplate();

        JsonNode program = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "New Platform Rollout",
                          "code": "PRG-ROLL-01",
                          "description": "Shared automotive rollout",
                          "ownerOrganizationId": "%s",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "participants": [
                            {
                              "organizationId": "%s",
                              "role": "SUPPLIER"
                            }
                          ],
                          "initialProject": {
                            "name": "Body Project",
                            "code": "PRJ-BODY-01",
                            "description": "Initial body project",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        ownerOrganizationId,
                        programStartDate,
                        programEndDate,
                        supplierOrganizationId,
                        projectStartDate,
                        projectEndDate,
                        milestoneTemplateId));

        String programId = program.get("id").asText();
        String projectId = program.get("projects").get(0).get("id").asText();

        JsonNode product = postForJson(
                "/api/portfolio/projects/" + projectId + "/products",
                """
                        {
                          "name": "Door Assembly",
                          "code": "PRD-DOOR-01",
                          "description": "Door family"
                        }
                        """);
        String productId = product.get("id").asText();

        JsonNode item = postForJson(
                "/api/portfolio/products/" + productId + "/items",
                """
                        {
                          "name": "Door Latch",
                          "code": "ITM-LATCH-01",
                          "description": "Primary latch item"
                        }
                        """);
        String itemId = item.get("id").asText();

        postForJson(
                "/api/portfolio/items/" + itemId + "/deliverables",
                """
                        {
                          "name": "PPAP Evidence",
                          "description": "Document package",
                          "type": "DOCUMENT",
                          "plannedDate": "%s",
                          "dueDate": "%s"
                        }
                        """.formatted(deliverablePlannedDate, deliverableDueDate));

        String deliverableId = mockMvc.perform(get("/api/portfolio/programs/" + programId).with(defaultJwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String resolvedDeliverableId = objectMapper.readTree(deliverableId)
                .get("projects").get(0)
                .get("products").get(0)
                .get("items").get(0)
                .get("deliverables").get(0)
                .get("id").asText();

        JsonNode preparedUpload = postForJson(
                "/api/portfolio/deliverables/" + resolvedDeliverableId + "/documents/upload-url",
                """
                        {
                          "fileName": "ppap-package.pdf",
                          "contentType": "application/pdf",
                          "fileSize": 1024
                        }
                        """);
        String documentId = preparedUpload.get("document").get("id").asText();

        postForJson(
                "/api/portfolio/deliverables/" + resolvedDeliverableId + "/documents/" + documentId + "/complete",
                "{}");

        postForJson(
                "/api/portfolio/deliverables/" + resolvedDeliverableId + "/documents/" + documentId + "/download-url",
                "{}");

        mockMvc.perform(get("/api/portfolio/deliverables/" + resolvedDeliverableId + "/documents").with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        postForJson(
                "/api/portfolio/programs/" + programId + "/open-issues",
                """
                        {
                          "title": "Supplier timeline pressure",
                          "description": "Macro issue for rollout",
                          "severity": "HIGH"
                        }
                        """);

        mockMvc.perform(get("/api/portfolio/programs/" + programId).with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRG-ROLL-01"))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.projects.length()").value(1))
                .andExpect(jsonPath("$.projects[0].code").value("PRJ-BODY-01"))
                .andExpect(jsonPath("$.projects[0].milestones.length()").value(2))
                .andExpect(jsonPath("$.projects[0].milestones[0].plannedDate").value(projectStartDate.toString()))
                .andExpect(jsonPath("$.projects[0].milestones[1].plannedDate").value(projectStartDate.plusWeeks(4).toString()))
                .andExpect(jsonPath("$.projects[0].products[0].items[0].deliverables[0].type").value("DOCUMENT"))
                .andExpect(jsonPath("$.projects[0].products[0].items[0].deliverables[0].documents.length()").value(1))
                .andExpect(jsonPath("$.projects[0].products[0].items[0].deliverables[0].documents[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.openIssues.length()").value(1))
                .andExpect(jsonPath("$.openIssues[0].severity").value("HIGH"));
    }

    private String createOrganization(String name, String code) throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/organizations",
                """
                        {
                          "name": "%s",
                          "code": "%s"
                        }
                        """.formatted(name, code));
        return response.get("id").asText();
    }

    private String createMilestoneTemplate() throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/milestone-templates",
                """
                        {
                          "name": "Standard Launch",
                          "description": "Reusable launch template",
                          "items": [
                            {
                              "name": "Kickoff",
                              "sortOrder": 1,
                              "required": true,
                              "offsetWeeks": 0
                            },
                            {
                              "name": "PPAP",
                              "sortOrder": 2,
                              "required": true,
                              "offsetWeeks": 4
                            }
                          ]
                        }
                        """);
        return response.get("id").asText();
    }

    private void createAdminUser(String organizationId, String email) throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Organization Admin",
                                  "email": "%s",
                                  "role": "ADMIN",
                                  "organizationId": "%s"
                                }
                                """.formatted(email, organizationId)))
                .andExpect(status().isCreated());
    }

    private JsonNode postForJson(String path, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultJwt() {
        return jwtFor("admin", "internal-core", "INTERNAL", "ADMIN");
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtFor(
            String username,
            String tenantId,
            String tenantType,
            String role) {
        return jwt().jwt(jwt -> jwt
                        .claim("sub", username + "-123")
                        .claim("cognito:username", username)
                        .claim("tenant_id", tenantId)
                        .claim("tenant_type", tenantType))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
