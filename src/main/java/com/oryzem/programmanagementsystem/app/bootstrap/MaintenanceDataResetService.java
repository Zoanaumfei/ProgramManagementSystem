package com.oryzem.programmanagementsystem.app.bootstrap;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentAdministrationFacade;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceDataResetService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceDataResetService.class);

    private static final List<String> RUNTIME_TABLES = List.of(
            "document_binding",
            "deliverable_submission_document",
            "deliverable_submission",
            "project_deliverable",
            "project_milestone",
            "project_structure_node",
            "project_phase",
            "project_member",
            "project_organization",
            "project_purge_intent",
            "project_idempotency",
            "project",
            "document");

    private static final List<String> BASELINE_PROJECT_TEMPLATE_IDS = List.of(
            "TMP-APQP-V1",
            "TMP-VDA-MLA-V1",
            "TMP-CUSTOM-V1");

    private static final List<String> BASELINE_STRUCTURE_TEMPLATE_IDS = List.of(
            "PST-APQP-V1",
            "PST-VDA-MLA-V1",
            "PST-CUSTOM-V1");

    private static final List<String> BASELINE_FRAMEWORK_CODES = List.of(
            "APQP",
            "VDA_MLA",
            "CUSTOM");

    private final JdbcTemplate jdbcTemplate;
    private final DocumentAdministrationFacade documentAdministrationFacade;

    public MaintenanceDataResetService(
            JdbcTemplate jdbcTemplate,
            DocumentAdministrationFacade documentAdministrationFacade) {
        this.jdbcTemplate = jdbcTemplate;
        this.documentAdministrationFacade = documentAdministrationFacade;
    }

    @Transactional
    public void clearRuntimeData() {
        documentAdministrationFacade.purgeAllDocumentsForMaintenanceReset();
        for (String tableName : RUNTIME_TABLES) {
            if (tableExists(tableName)) {
                int deletedRows = jdbcTemplate.update("DELETE FROM " + tableName);
                log.info("Cleared runtime table '{}' rows={}", tableName, deletedRows);
            }
        }
        clearNonBaselineProjectCatalogData();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE lower(table_name) = lower(?)
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private void clearNonBaselineProjectCatalogData() {
        if (tableExists("project_template")) {
            int deletedTemplates = jdbcTemplate.update("""
                    DELETE FROM project_template
                    WHERE id NOT IN (?, ?, ?)
                       OR owner_organization_id <> 'internal-core'
                       OR is_default <> TRUE
                    """, BASELINE_PROJECT_TEMPLATE_IDS.toArray());
            log.info("Cleared non-baseline project templates rows={}", deletedTemplates);
        }

        if (tableExists("project_structure_template")) {
            int deletedStructureTemplates = jdbcTemplate.update("""
                    DELETE FROM project_structure_template
                    WHERE id NOT IN (?, ?, ?)
                       OR owner_organization_id <> 'internal-core'
                    """, BASELINE_STRUCTURE_TEMPLATE_IDS.toArray());
            log.info("Cleared non-baseline project structure templates rows={}", deletedStructureTemplates);
        }

        if (tableExists("project_framework")) {
            int deletedFrameworks = jdbcTemplate.update("""
                    DELETE FROM project_framework
                    WHERE code NOT IN (?, ?, ?)
                    """, BASELINE_FRAMEWORK_CODES.toArray());
            log.info("Cleared non-baseline project frameworks rows={}", deletedFrameworks);
        }
    }
}
