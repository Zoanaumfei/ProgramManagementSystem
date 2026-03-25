package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
@AutoConfigureMockMvc
class PortfolioDocumentS3SmokeTest {

    private static final String SMOKE_BUCKET = System.getenv("PMS_DOCUMENTS_SMOKE_BUCKET");
    private static final String SMOKE_REGION = System.getenv().getOrDefault("PMS_DOCUMENTS_SMOKE_REGION", "sa-east-1");

    @DynamicPropertySource
    static void documentStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.portfolio.documents.provider", () -> hasSmokeBucket() ? "s3" : "stub");
        registry.add("app.portfolio.documents.bucket-name", () -> hasSmokeBucket() ? SMOKE_BUCKET : "");
        registry.add("app.portfolio.documents.key-prefix", () -> "portfolio-smoke");
        registry.add("app.portfolio.documents.region", () -> SMOKE_REGION);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataService bootstrapDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        bootstrapDataService.reset();
    }

    @Test
    void shouldExecuteGoldenFlowAgainstRealS3AndRespectSubtreeVisibility() throws Exception {
        Assumptions.assumeTrue(hasSmokeBucket(), "Set PMS_DOCUMENTS_SMOKE_BUCKET to run the real S3 smoke test.");

        String customerId = createOrganization("Golden Flow Customer", "GF-CUST");
        String siblingCustomerId = createOrganization("Isolated Customer", "GF-ISO");
        String rootAdminUserId = createAdminUser(defaultJwt(), customerId, "root.admin@golden-flow.com");
        Assertions.assertThat(rootAdminUserId).isNotBlank();

        String childOrganizationId = createChildOrganization(
                jwtFor("root-admin", customerId, "EXTERNAL", "ADMIN"),
                customerId,
                "Golden Flow Tier 1",
                "GF-T1");
        String childAdminUserId = createAdminUser(
                jwtFor("root-admin", customerId, "EXTERNAL", "ADMIN"),
                childOrganizationId,
                "child.admin@golden-flow.com");
        Assertions.assertThat(childAdminUserId).isNotBlank();
        createAdminUser(defaultJwt(), siblingCustomerId, "isolated.admin@golden-flow.com");

        String milestoneTemplateId = createMilestoneTemplate();
        JsonNode program = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "Golden Flow Program",
                          "code": "GF-PRG-01",
                          "description": "Real S3 smoke flow",
                          "ownerOrganizationId": "%s",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "initialProject": {
                            "name": "Golden Flow Project",
                            "code": "GF-PRJ-01",
                            "description": "Child-owned project",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        childOrganizationId,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusMonths(1),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusWeeks(4),
                        milestoneTemplateId),
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));

        String programId = program.get("id").asText();
        String projectId = program.get("projects").get(0).get("id").asText();

        JsonNode product = postForJson(
                "/api/portfolio/projects/" + projectId + "/products",
                """
                        {
                          "name": "Golden Product",
                          "code": "GF-PRD-01",
                          "description": "Product for real S3 flow"
                        }
                        """,
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));
        String productId = product.get("id").asText();

        JsonNode item = postForJson(
                "/api/portfolio/products/" + productId + "/items",
                """
                        {
                          "name": "Golden Item",
                          "code": "GF-ITM-01",
                          "description": "Item for document upload"
                        }
                        """,
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));
        String itemId = item.get("id").asText();

        JsonNode deliverable = postForJson(
                "/api/portfolio/items/" + itemId + "/deliverables",
                """
                        {
                          "name": "Golden Deliverable",
                          "description": "Document-backed deliverable",
                          "type": "DOCUMENT",
                          "plannedDate": "%s",
                          "dueDate": "%s"
                        }
                        """.formatted(LocalDate.now().plusDays(3), LocalDate.now().plusDays(10)),
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));
        String deliverableId = deliverable.get("id").asText();

        byte[] expectedContent = "golden-flow-s3-smoke".getBytes(StandardCharsets.UTF_8);
        JsonNode preparedUpload = postForJson(
                "/api/portfolio/deliverables/" + deliverableId + "/documents/upload-url",
                """
                        {
                          "fileName": "golden-flow-proof.txt",
                          "contentType": "text/plain",
                          "fileSize": %s
                        }
                        """.formatted(expectedContent.length),
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));
        String documentId = preparedUpload.get("document").get("id").asText();

        uploadPreparedDocument(preparedUpload, expectedContent);

        postForJson(
                "/api/portfolio/deliverables/" + deliverableId + "/documents/" + documentId + "/complete",
                "{}",
                jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN"));

        mockMvc.perform(get("/api/portfolio/deliverables/" + deliverableId + "/documents")
                        .with(jwtFor("root-admin", customerId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        JsonNode preparedDownload = postForJson(
                "/api/portfolio/deliverables/" + deliverableId + "/documents/" + documentId + "/download-url",
                "{}",
                jwtFor("root-admin", customerId, "EXTERNAL", "ADMIN"));

        HttpResponse<byte[]> downloadResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(preparedDownload.get("downloadUrl").asText()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        Assertions.assertThat(downloadResponse.statusCode()).isEqualTo(200);
        Assertions.assertThat(downloadResponse.body()).isEqualTo(expectedContent);

        mockMvc.perform(get("/api/portfolio/programs")
                        .with(jwtFor("root-admin", customerId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerOrganizationId").value(childOrganizationId));

        mockMvc.perform(get("/api/portfolio/programs/" + programId)
                        .with(jwtFor("child-admin", childOrganizationId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId));

        mockMvc.perform(get("/api/portfolio/programs")
                        .with(jwtFor("isolated-admin", siblingCustomerId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/portfolio/organizations/" + customerId + "/purge-subtree")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup after real S3 smoke validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purgedOrganizations").value(2))
                .andExpect(jsonPath("$.purgedPrograms").value(1))
                .andExpect(jsonPath("$.purgedDocuments").value(1));
    }

    private void uploadPreparedDocument(JsonNode preparedUpload, byte[] content) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(preparedUpload.get("uploadUrl").asText()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        JsonNode requiredHeaders = preparedUpload.get("requiredHeaders");
        requiredHeaders.properties().forEach(header -> requestBuilder.header(header.getKey(), header.getValue().textValue()));

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertThat(response.statusCode()).isIn(200, 201);
    }

    private String createOrganization(String name, String code) throws Exception {
        return createOrganization(defaultJwt(), name, code);
    }

    private String createOrganization(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String name,
            String code) throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/organizations",
                """
                        {
                          "name": "%s",
                          "code": "%s"
                        }
                        """.formatted(name, code),
                actor);
        return response.get("id").asText();
    }

    private String createChildOrganization(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String parentOrganizationId,
            String name,
            String code) throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/organizations",
                """
                        {
                          "name": "%s",
                          "code": "%s",
                          "parentOrganizationId": "%s"
                        }
                        """.formatted(name, code, parentOrganizationId),
                actor);
        return response.get("id").asText();
    }

    private String createMilestoneTemplate() throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/milestone-templates",
                """
                        {
                          "name": "Golden Flow Template",
                          "description": "Smoke template",
                          "items": [
                            {
                              "name": "Kickoff",
                              "sortOrder": 1,
                              "required": true,
                              "offsetWeeks": 0
                            }
                          ]
                        }
                        """,
                defaultJwt());
        return response.get("id").asText();
    }

    private String createAdminUser(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor,
            String organizationId,
            String email) throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .with(actor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Golden Flow Admin",
                                  "email": "%s",
                                  "role": "ADMIN",
                                  "organizationId": "%s"
                                }
                                """.formatted(email, organizationId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private JsonNode postForJson(
            String path,
            String json,
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor)
            throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .with(actor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static boolean hasSmokeBucket() {
        return SMOKE_BUCKET != null && !SMOKE_BUCKET.isBlank();
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultJwt() {
        return jwtFor("internal-admin", "internal-core", "INTERNAL", "ADMIN");
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
