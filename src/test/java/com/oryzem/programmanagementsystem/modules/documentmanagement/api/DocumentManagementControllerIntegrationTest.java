package com.oryzem.programmanagementsystem.modules.documentmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStorage;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.DocumentEntity;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentBindingJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.infrastructure.SpringDataDocumentJpaRepository;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.DocumentChecksumSupport;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.InMemoryDocumentStorageStub;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.TestProjectDocumentContextPolicyProvider;
import jakarta.persistence.EntityManager;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class,
        properties = "app.project-management.enabled=false")
@AutoConfigureMockMvc
@Import(DocumentManagementControllerIntegrationTest.DocumentManagementTestConfiguration.class)
class DocumentManagementControllerIntegrationTest {

    @TestConfiguration
    static class DocumentManagementTestConfiguration {

        @Bean
        @Primary
        InMemoryDocumentStorageStub documentStorage() {
            return new InMemoryDocumentStorageStub();
        }

        @Bean
        DocumentContextPolicyProvider projectDocumentContextPolicyProvider() {
            return new TestProjectDocumentContextPolicyProvider();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private SpringDataDocumentBindingJpaRepository bindingRepository;

    @Autowired
    private SpringDataDocumentJpaRepository documentRepository;

    @Autowired
    private DocumentStorage documentStorage;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        stubStorage().clear();
        bindingRepository.deleteAll();
        documentRepository.deleteAll();
        bootstrapDataService.reset();
    }

    @Test
    void shouldInitiateAuthorizedUpload() throws Exception {
        byte[] content = pdfBytes();
        mockMvc.perform(post("/api/document-contexts/PROJECT/project-123/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").isNotEmpty())
                .andExpect(jsonPath("$.url").value("https://upload.test/" + documentRepository.findAll().getFirst().getStorageKey()))
                .andExpect(jsonPath("$.fields.key").isNotEmpty())
                .andExpect(jsonPath("$.fields.Content-Type").value("application/pdf"));

        assertThat(documentRepository.findAll())
                .singleElement()
                .extracting(DocumentEntity::getStatus)
                .isEqualTo(com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus.PENDING_UPLOAD);
    }

    @Test
    void shouldBlockInitiateUploadWhenPolicyDenies() throws Exception {
        mockMvc.perform(post("/api/document-contexts/PROJECT/project-read-only/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", pdfBytes())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFinalizeUploadWhenObjectExists() throws Exception {
        String documentId = initiateDocument("project-123");
        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        byte[] content = pdfBytes();
        stubStorage().putObject(
                document.getStorageKey(),
                document.getContentType(),
                content,
                Map.of(
                        "checksum-sha256", document.getChecksumSha256(),
                        "document-id", document.getId()));

        mockMvc.perform(post("/api/documents/" + documentId + "/finalize-upload")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldMarkUploadFailedWhenObjectIsMissing() throws Exception {
        String documentId = initiateDocument("project-123");

        mockMvc.perform(post("/api/documents/" + documentId + "/finalize-upload")
                        .with(externalAdminTenantA()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_UPLOAD_OBJECT_NOT_FOUND"));

        entityManager.clear();
        assertThat(documentRepository.findById(documentId).orElseThrow().getStatus())
                .isEqualTo(com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus.FAILED);
    }

    @Test
    void shouldListDocumentsByContext() throws Exception {
        String activeDocumentId = initiateDocument("project-list");
        DocumentEntity activeDocument = documentRepository.findById(activeDocumentId).orElseThrow();
        stubStorage().putObject(
                activeDocument.getStorageKey(),
                activeDocument.getContentType(),
                pdfBytes(),
                Map.of("checksum-sha256", activeDocument.getChecksumSha256(), "document-id", activeDocument.getId()));
        mockMvc.perform(post("/api/documents/" + activeDocumentId + "/finalize-upload")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk());

        initiateDocument("project-list");

        mockMvc.perform(get("/api/document-contexts/PROJECT/project-list/documents")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGenerateAuthorizedDownloadUrl() throws Exception {
        String documentId = initiateAndFinalize("project-123");

        mockMvc.perform(post("/api/documents/" + documentId + "/download-url")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://download.test/" + documentRepository.findById(documentId).orElseThrow().getStorageKey()))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void shouldBlockDownloadWithoutPermission() throws Exception {
        String documentId = initiateAndFinalize("project-no-download");

        mockMvc.perform(post("/api/documents/" + documentId + "/download-url")
                        .with(externalAdminTenantA()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIsolateTenantAcrossContexts() throws Exception {
        mockMvc.perform(post("/api/document-contexts/PROJECT/project-tenant-b/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", pdfBytes())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockNonexistentContext() throws Exception {
        mockMvc.perform(post("/api/document-contexts/PROJECT/project-missing/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", pdfBytes())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldBlockFeatureDisabledContext() throws Exception {
        mockMvc.perform(post("/api/document-contexts/PROJECT/project-disabled/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", pdfBytes())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldSoftDeleteDocumentWhenAuthorized() throws Exception {
        String documentId = initiateAndFinalize("project-123");

        mockMvc.perform(delete("/api/documents/" + documentId)
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    private String initiateAndFinalize(String contextId) throws Exception {
        String documentId = initiateDocument(contextId);
        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        stubStorage().putObject(
                document.getStorageKey(),
                document.getContentType(),
                pdfBytes(),
                Map.of("checksum-sha256", document.getChecksumSha256(), "document-id", document.getId()));
        mockMvc.perform(post("/api/documents/" + documentId + "/finalize-upload")
                        .with(externalAdminTenantA()))
                .andExpect(status().isOk());
        return documentId;
    }

    private String initiateDocument(String contextId) throws Exception {
        String response = mockMvc.perform(post("/api/document-contexts/PROJECT/" + contextId + "/documents/uploads")
                        .with(externalAdminTenantA())
                        .contentType(APPLICATION_JSON)
                        .content(initiateRequest("evidencia.pdf", "application/pdf", pdfBytes())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("documentId").asText();
    }

    private String initiateRequest(String filename, String contentType, byte[] content) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "originalFilename", filename,
                "contentType", contentType,
                "sizeBytes", content.length,
                "checksumSha256", DocumentChecksumSupport.sha256Hex(content)));
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\noryzem\n".getBytes(StandardCharsets.US_ASCII);
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
}
