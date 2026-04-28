package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import java.util.List;

/**
 * Published administrative contract exposed by document-management to other modules.
 */
public interface DocumentAdministrationFacade {

    DocumentPurgeSummary summarizeTrackedDocuments(List<DocumentContextRef> contexts);

    DocumentPurgeSummary purgeTrackedDocuments(List<DocumentContextRef> contexts);

    DocumentPurgeSummary purgeAllDocumentsForMaintenanceReset();
}
