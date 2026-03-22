package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.oryzem.programmanagementsystem.app.ProgramManagementSystemApplication.class)
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
                .andExpect(jsonPath("$.tenantType").value("EXTERNAL"))
                .andExpect(jsonPath("$.hierarchyLevel").value(0))
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
                .andExpect(jsonPath("$.message").value("Only INTERNAL admins can create root customer organizations."));
    }

    @Test
    void externalAdminShouldCreateChildOrganizationOnlyInsideOwnHierarchy() throws Exception {
        String tier1Id = createChildOrganization("tenant-a", "Tenant A Tier 1", "TENANT-A-T1");

        mockMvc.perform(post("/api/portfolio/organizations")
                        .with(jwtFor("tenant-a-admin", "tenant-a", "EXTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tenant A Tier 2",
                                  "code": "TENANT-A-T2",
                                  "parentOrganizationId": "%s"
                                }
                                """.formatted(tier1Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentOrganizationId").value(tier1Id))
                .andExpect(jsonPath("$.customerOrganizationId").value("tenant-a"))
                .andExpect(jsonPath("$.hierarchyLevel").value(2));

        mockMvc.perform(post("/api/portfolio/organizations")
                        .with(jwtFor("tenant-a-admin", "tenant-a", "EXTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Foreign Tier",
                                  "code": "TENANT-B-T1",
                                  "parentOrganizationId": "tenant-b"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Organization is outside the manageable hierarchy for the authenticated user."));
    }

    @Test
    void supportAndManagerShouldRespectOrganizationVisibilityRules() throws Exception {
        String customerId = createOrganization("Support Customer", "SUP-CUST");
        String childOrganizationId = createChildOrganization(customerId, "Support Tier 1", "SUP-T1");

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(customerId)).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(childOrganizationId)).exists())
                .andExpect(jsonPath("$[?(@.id == 'internal-core')]").doesNotExist());

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(jwtFor("external-support", "tenant-a", "EXTERNAL", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("tenant-a"));

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(jwtFor("tenant-a-manager", "tenant-a", "EXTERNAL", "MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No base permission")));
    }

    @Test
    void externalAdminShouldManageOrganizationsOnlyInsideOwnHierarchy() throws Exception {
        String childOrganizationId = createChildOrganization("tenant-a", "Tenant A Child", "TENANT-A-CHILD");
        String organizationId = createOrganization("Supplier X", "SUP-X");

        mockMvc.perform(put("/api/portfolio/organizations/" + childOrganizationId)
                        .with(jwtFor("tenant-a-admin", "tenant-a", "EXTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tenant A Child Updated",
                                  "code": "TENANT-A-CHILD-UPD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(childOrganizationId))
                .andExpect(jsonPath("$.name").value("Tenant A Child Updated"));

        mockMvc.perform(delete("/api/portfolio/organizations/" + childOrganizationId)
                        .with(jwtFor("tenant-a-admin", "tenant-a", "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(put("/api/portfolio/organizations/" + organizationId)
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Supplier X Updated",
                                  "code": "SUP-X-UPDATED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.name").value("Supplier X Updated"))
                .andExpect(jsonPath("$.code").value("SUP-X-UPDATED"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/portfolio/organizations/" + organizationId)
                        .with(jwtFor("tenant-a-admin", "tenant-a", "EXTERNAL", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Should Not Work",
                                  "code": "SUP-X-BLOCKED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Organization is outside the manageable hierarchy for the authenticated user."));
    }

    @Test
    void internalSupportShouldPurgeOrganizationSubtreeWhenExplicitlyConfirmed() throws Exception {
        String customerId = createOrganization("Purge Customer", "PURGE-CUST");
        String childOrganizationId = createChildOrganization(customerId, "Purge Tier 1", "PURGE-T1");
        createAdminUser(customerId, "admin@purge-customer.com");
        createAdminUser(childOrganizationId, "admin@purge-tier1.com");
        String milestoneTemplateId = createMilestoneTemplate();
        createProgram(customerId, milestoneTemplateId, "PRG-PURGE-01", "PRJ-PURGE-01", null);
        createProgram(childOrganizationId, milestoneTemplateId, "PRG-PURGE-02", "PRJ-PURGE-02", null);

        mockMvc.perform(post("/api/portfolio/organizations/" + customerId + "/purge-subtree")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of test customer tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(customerId))
                .andExpect(jsonPath("$.action").value("PURGE"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.purgedOrganizations").value(2))
                .andExpect(jsonPath("$.purgedPrograms").value(2))
                .andExpect(jsonPath("$.purgedUsers").value(2));

        mockMvc.perform(get("/api/portfolio/organizations/" + customerId).with(defaultJwt()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/users")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .param("organizationId", childOrganizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void organizationPurgeShouldRequireInternalSupportAndExplicitConfirmation() throws Exception {
        String organizationId = createOrganization("Support Purge Only", "SUP-PURGE-ONLY");

        mockMvc.perform(post("/api/portfolio/organizations/" + organizationId + "/purge-subtree")
                        .with(jwtFor("external-support", "tenant-a", "EXTERNAL", "SUPPORT"))
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only INTERNAL SUPPORT can purge organization subtrees."));

        mockMvc.perform(post("/api/portfolio/organizations/" + organizationId + "/purge-subtree")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .param("justification", "Cleanup"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization purge requires supportOverride=true."));
    }

    @Test
    void organizationPurgeShouldRejectSubtreesReferencedByExternalPrograms() throws Exception {
        String customerId = createOrganization("Shared Customer", "SHARED-CUST");
        String sourceOrganizationId = createChildOrganization(customerId, "Source Tier", "SRC-T1");
        String siblingOrganizationId = createChildOrganization(customerId, "Sibling Tier", "SIB-T1");
        String milestoneTemplateId = createMilestoneTemplate();
        createAdminUser(siblingOrganizationId, "admin@sibling-tier.com");
        createProgram(siblingOrganizationId, milestoneTemplateId, "PRG-SHARED-01", "PRJ-SHARED-01", sourceOrganizationId);

        mockMvc.perform(post("/api/portfolio/organizations/" + sourceOrganizationId + "/purge-subtree")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .param("supportOverride", "true")
                        .param("justification", "Cleanup of isolated test subtree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Organization subtree cannot be purged while it still participates in programs owned outside the subtree."));
    }

    @Test
    void shouldInactivateOrganizationWhenItHasNoInvitedOrActiveUsers() throws Exception {
        String organizationId = createOrganization("Supplier Z", "SUP-Z");

        mockMvc.perform(delete("/api/portfolio/organizations/" + organizationId)
                        .with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/portfolio/organizations/" + organizationId).with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldRejectOrganizationInactivationWhenInvitedOrActiveUsersStillExist() throws Exception {
        String organizationId = createOrganization("Supplier Keep", "SUP-KEEP");
        createAdminUser(organizationId, "admin@supplier-keep.com");

        mockMvc.perform(delete("/api/portfolio/organizations/" + organizationId)
                        .with(defaultJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Organization can only be inactivated after all invited or active users are inactivated."));
    }

    @Test
    void shouldRejectProgramCreationWhenOwnerOrganizationIsInactive() throws Exception {
        String ownerOrganizationId = createOrganization("Oryzem Internal", "ORY-INT");
        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(delete("/api/portfolio/organizations/" + ownerOrganizationId)
                        .with(defaultJwt()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Inactive Org Program",
                                  "code": "PRG-INACTIVE-01",
                                  "description": "Should fail while org is inactive",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Initial Project",
                                    "code": "PRJ-INACTIVE-01",
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
                .andExpect(jsonPath("$.message").value("Owner organization is inactive."));
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
    void shouldFilterOrganizationsAndExposeManagementSignals() throws Exception {
        String ownerOrganizationId = createOrganization("Filter Owner", "FLT-OWN");
        String supplierOrganizationId = createChildOrganization(ownerOrganizationId, "Supplier Filter", "FLT-SUP");
        String archivedOrganizationId = createChildOrganization(ownerOrganizationId, "Archived Supplier", "ARC-SUP");

        createAdminUser(ownerOrganizationId, "owner@filter.com");
        createAdminUser(supplierOrganizationId, "supplier.admin@filter.com");

        mockMvc.perform(delete("/api/portfolio/organizations/" + archivedOrganizationId)
                        .with(defaultJwt()))
                .andExpect(status().isOk());

        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Organization Filter Program",
                                  "code": "PRG-FILTER-01",
                                  "description": "Program for organization filters",
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
                                    "name": "Filter Project",
                                    "code": "PRJ-FILTER-01",
                                    "description": "Filter project",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                ownerOrganizationId,
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                supplierOrganizationId,
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(defaultJwt())
                        .param("status", "ACTIVE")
                        .param("setupStatus", "COMPLETED")
                        .param("search", "supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(supplierOrganizationId))
                .andExpect(jsonPath("$[0].name").value("Supplier Filter"))
                .andExpect(jsonPath("$[0].setupStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].userSummary.invitedCount").value(1))
                .andExpect(jsonPath("$[0].userSummary.activeCount").value(0))
                .andExpect(jsonPath("$[0].programSummary.ownedCount").value(0))
                .andExpect(jsonPath("$[0].programSummary.participatingCount").value(1))
                .andExpect(jsonPath("$[0].programSummary.totalCount").value(1))
                .andExpect(jsonPath("$[0].canInactivate").value(false))
                .andExpect(jsonPath("$[0].inactivationBlockedReason").value(
                        "Organization can only be inactivated after all invited or active users are inactivated."));

        mockMvc.perform(get("/api/portfolio/organizations/" + archivedOrganizationId)
                        .with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.canInactivate").value(false))
                .andExpect(jsonPath("$.inactivationBlockedReason").value("Organization is already inactive."));
    }

    @Test
    void shouldHideInternalOrganizationsAndSupportHierarchyFilters() throws Exception {
        String customerId = createOrganization("Hierarchy Customer", "HIER-CUST");
        String tier1Id = createChildOrganization(customerId, "Hierarchy Tier 1", "HIER-T1");
        String tier2Id = createChildOrganization(tier1Id, "Hierarchy Tier 2", "HIER-T2");

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(defaultJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'internal-core')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(customerId)).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(tier1Id)).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(tier2Id)).exists());

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(defaultJwt())
                        .param("customerOrganizationId", customerId)
                        .param("hierarchyLevel", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tier1Id));

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(defaultJwt())
                        .param("parentOrganizationId", tier1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tier2Id))
                .andExpect(jsonPath("$[0].customerOrganizationId").value(customerId));
    }

    @Test
    void shouldRestrictVisibleOrganizationsAndProgramsToOwnHierarchy() throws Exception {
        String customerAId = createOrganization("Customer A", "CUST-A");
        String customerBId = createOrganization("Customer B", "CUST-B");
        String tier1AId = createChildOrganization(customerAId, "Tier 1 A", "CUST-A-T1");
        createAdminUser(customerAId, "customer.a.admin@oryzem.com");
        createAdminUser(tier1AId, "tier1.a.admin@oryzem.com");

        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tier Program",
                                  "code": "PRG-TIER-A-01",
                                  "description": "Tier hierarchy program",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Tier Project",
                                    "code": "PRJ-TIER-A-01",
                                    "description": "Tier project",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                tier1AId,
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/organizations")
                        .with(jwtFor("customer-a-admin", customerAId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(customerAId)).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(tier1AId)).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(customerBId)).doesNotExist());

        mockMvc.perform(get("/api/portfolio/programs")
                        .with(jwtFor("customer-a-admin", customerAId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerOrganizationId").value(tier1AId));

        mockMvc.perform(get("/api/portfolio/programs")
                        .with(jwtFor("customer-b-admin", customerBId, "EXTERNAL", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFilterProgramsByOwnerOrganizationAndRejectInternalOwner() throws Exception {
        String customerId = createOrganization("Program Customer", "PRG-CUST");
        String tier1Id = createChildOrganization(customerId, "Program Tier 1", "PRG-T1");
        String tier2Id = createChildOrganization(tier1Id, "Program Tier 2", "PRG-T2");
        createAdminUser(tier1Id, "program.tier1.admin@oryzem.com");
        createAdminUser(tier2Id, "program.tier2.admin@oryzem.com");
        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tier 1 Program",
                                  "code": "PRG-TIER1-01",
                                  "description": "Owned by tier 1",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Tier 1 Project",
                                    "code": "PRJ-TIER1-01",
                                    "description": "Tier 1 project",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                tier1Id,
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tier 2 Program",
                                  "code": "PRG-TIER2-01",
                                  "description": "Owned by tier 2",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Tier 2 Project",
                                    "code": "PRJ-TIER2-01",
                                    "description": "Tier 2 project",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                tier2Id,
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusMonths(2),
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusWeeks(5),
                                milestoneTemplateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/programs")
                        .with(defaultJwt())
                        .param("ownerOrganizationId", tier1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerOrganizationId").value(tier1Id));

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Internal Program",
                                  "code": "PRG-INTERNAL-01",
                                  "description": "Should fail",
                                  "ownerOrganizationId": "internal-core",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Internal Project",
                                    "code": "PRJ-INTERNAL-01",
                                    "description": "Should fail",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization 'internal-core' was not found."));
    }

    @Test
    void shouldRejectOrganizationInactivationWhenActiveProjectsStillExist() throws Exception {
        String ownerOrganizationId = createOrganization("Owner Active Project", "OWN-ACT");
        String ownerAdminUserId = createAdminUser(ownerOrganizationId, "owner.active.project@oryzem.com");
        String milestoneTemplateId = createMilestoneTemplate();

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(defaultJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Active Program",
                                  "code": "PRG-ACTIVE-01",
                                  "description": "Has active project",
                                  "ownerOrganizationId": "%s",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Active Project",
                                    "code": "PRJ-ACTIVE-01",
                                    "description": "Active project",
                                    "status": "ACTIVE",
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
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/" + ownerAdminUserId)
                        .with(defaultJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/portfolio/organizations/" + ownerOrganizationId)
                        .with(defaultJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Organization can only be inactivated after all active projects are closed or inactivated."));
    }

    @Test
    void managerShouldManageProjectLayerButNotPrograms() throws Exception {
        String milestoneTemplateId = createMilestoneTemplate();
        JsonNode program = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "Manager Scope Program",
                          "code": "PRG-MGR-01",
                          "description": "Program for manager permissions",
                          "ownerOrganizationId": "tenant-a",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "initialProject": {
                            "name": "Initial Manager Project",
                            "code": "PRJ-MGR-INIT-01",
                            "description": "Initial project",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusMonths(1),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusWeeks(4),
                        milestoneTemplateId));

        String programId = program.get("id").asText();
        String projectId = program.get("projects").get(0).get("id").asText();

        mockMvc.perform(post("/api/portfolio/programs/" + programId + "/projects")
                        .with(jwtFor("tenant-a-manager", "tenant-a", "EXTERNAL", "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Manager Created Project",
                                  "code": "PRJ-MGR-NEW-01",
                                  "description": "Created by manager",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "milestoneTemplateId": "%s"
                                }
                                """.formatted(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusWeeks(6),
                                milestoneTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRJ-MGR-NEW-01"));

        mockMvc.perform(post("/api/portfolio/projects/" + projectId + "/products")
                        .with(jwtFor("tenant-a-manager", "tenant-a", "EXTERNAL", "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Manager Product",
                                  "code": "PRD-MGR-01",
                                  "description": "Created by manager"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRD-MGR-01"));

        mockMvc.perform(post("/api/portfolio/programs/" + programId + "/open-issues")
                        .with(jwtFor("tenant-a-manager", "tenant-a", "EXTERNAL", "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Manager Open Issue",
                                  "description": "Raised by manager",
                                  "severity": "HIGH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Manager Open Issue"));

        mockMvc.perform(post("/api/portfolio/programs")
                        .with(jwtFor("tenant-a-manager", "tenant-a", "EXTERNAL", "MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Manager Program",
                                  "code": "PRG-MGR-BLOCK-01",
                                  "description": "Should be denied",
                                  "ownerOrganizationId": "tenant-a",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "initialProject": {
                                    "name": "Blocked Project",
                                    "code": "PRJ-MGR-BLOCK-01",
                                    "description": "Blocked",
                                    "plannedStartDate": "%s",
                                    "plannedEndDate": "%s",
                                    "milestoneTemplateId": "%s"
                                  }
                                }
                                """.formatted(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusMonths(1),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusWeeks(4),
                                milestoneTemplateId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only ADMIN can manage programs."));
    }

    @Test
    void memberShouldManageExecutionLayerButNotProjectLayer() throws Exception {
        String milestoneTemplateId = createMilestoneTemplate();
        JsonNode program = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "Member Scope Program",
                          "code": "PRG-MEM-01",
                          "description": "Program for member permissions",
                          "ownerOrganizationId": "tenant-a",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "initialProject": {
                            "name": "Initial Member Project",
                            "code": "PRJ-MEM-INIT-01",
                            "description": "Initial project",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusMonths(1),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusWeeks(4),
                        milestoneTemplateId));

        String programId = program.get("id").asText();
        String projectId = program.get("projects").get(0).get("id").asText();

        mockMvc.perform(post("/api/portfolio/programs/" + programId + "/projects")
                        .with(jwtFor("tenant-a-member", "tenant-a", "EXTERNAL", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Member Project",
                                  "code": "PRJ-MEM-BLOCK-01",
                                  "description": "Should be denied",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "milestoneTemplateId": "%s"
                                }
                                """.formatted(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusWeeks(6),
                                milestoneTemplateId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only ADMIN or MANAGER can manage projects, products and open issues."));

        mockMvc.perform(post("/api/portfolio/projects/" + projectId + "/products")
                        .with(jwtFor("tenant-a-member", "tenant-a", "EXTERNAL", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Member Product",
                                  "code": "PRD-MEM-BLOCK-01",
                                  "description": "Should be denied"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only ADMIN or MANAGER can manage projects, products and open issues."));

        JsonNode product = postForJson(
                "/api/portfolio/projects/" + projectId + "/products",
                """
                        {
                          "name": "Admin Product",
                          "code": "PRD-MEM-01",
                          "description": "Created by admin"
                        }
                        """);
        String productId = product.get("id").asText();

        JsonNode itemResponse = objectMapper.readTree(mockMvc.perform(post("/api/portfolio/products/" + productId + "/items")
                        .with(jwtFor("tenant-a-member", "tenant-a", "EXTERNAL", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Member Item",
                                  "code": "ITM-MEM-01",
                                  "description": "Created by member"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITM-MEM-01"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        String itemId = itemResponse.get("id").asText();

        JsonNode deliverableResponse = objectMapper.readTree(mockMvc.perform(post("/api/portfolio/items/" + itemId + "/deliverables")
                        .with(jwtFor("tenant-a-member", "tenant-a", "EXTERNAL", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Member Deliverable",
                                  "description": "Created by member",
                                  "type": "DOCUMENT",
                                  "plannedDate": "%s",
                                  "dueDate": "%s"
                                }
                                """.formatted(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Member Deliverable"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        String deliverableId = deliverableResponse.get("id").asText();

        mockMvc.perform(post("/api/portfolio/deliverables/" + deliverableId + "/documents/upload-url")
                        .with(jwtFor("tenant-a-member", "tenant-a", "EXTERNAL", "MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "member-evidence.pdf",
                                  "contentType": "application/pdf",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.status").value("PENDING_UPLOAD"));
    }

    @Test
    void supportShouldRemainReadOnlyInPortfolio() throws Exception {
        String milestoneTemplateId = createMilestoneTemplate();
        JsonNode program = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "Support View Program",
                          "code": "PRG-SUP-01",
                          "description": "Program for support permissions",
                          "ownerOrganizationId": "tenant-a",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "initialProject": {
                            "name": "Initial Support Project",
                            "code": "PRJ-SUP-INIT-01",
                            "description": "Initial project",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusMonths(1),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusWeeks(4),
                        milestoneTemplateId));

        String programId = program.get("id").asText();

        mockMvc.perform(get("/api/portfolio/programs/" + programId)
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId));

        mockMvc.perform(post("/api/portfolio/programs/" + programId + "/projects")
                        .with(jwtFor("internal-support", "internal-core", "INTERNAL", "SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Support Project",
                                  "code": "PRJ-SUP-BLOCK-01",
                                  "description": "Should be denied",
                                  "plannedStartDate": "%s",
                                  "plannedEndDate": "%s",
                                  "milestoneTemplateId": "%s"
                                }
                                """.formatted(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusWeeks(6),
                                milestoneTemplateId)))
                .andExpect(status().isForbidden());
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
        String supplierOrganizationId = createChildOrganization(ownerOrganizationId, "Supplier A", "SUP-A");
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

    private String createChildOrganization(String parentOrganizationId, String name, String code) throws Exception {
        JsonNode response = postForJson(
                "/api/portfolio/organizations",
                """
                        {
                          "name": "%s",
                          "code": "%s",
                          "parentOrganizationId": "%s"
                        }
                        """.formatted(name, code, parentOrganizationId));
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

    private String createAdminUser(String organizationId, String email) throws Exception {
        String response = mockMvc.perform(post("/api/users")
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
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createProgram(
            String ownerOrganizationId,
            String milestoneTemplateId,
            String programCode,
            String projectCode,
            String participantOrganizationId) throws Exception {
        String participantsJson = participantOrganizationId == null
                ? "[]"
                : """
                        [
                          {
                            "organizationId": "%s",
                            "role": "SUPPLIER"
                          }
                        ]
                        """.formatted(participantOrganizationId);

        JsonNode response = postForJson(
                "/api/portfolio/programs",
                """
                        {
                          "name": "%s",
                          "code": "%s",
                          "description": "Program for purge tests",
                          "ownerOrganizationId": "%s",
                          "plannedStartDate": "%s",
                          "plannedEndDate": "%s",
                          "participants": %s,
                          "initialProject": {
                            "name": "%s",
                            "code": "%s",
                            "description": "Initial project for purge tests",
                            "plannedStartDate": "%s",
                            "plannedEndDate": "%s",
                            "milestoneTemplateId": "%s"
                          }
                        }
                        """.formatted(
                        programCode,
                        programCode,
                        ownerOrganizationId,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusMonths(1),
                        participantsJson,
                        projectCode,
                        projectCode,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusWeeks(4),
                        milestoneTemplateId));
        return response.get("id").asText();
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



