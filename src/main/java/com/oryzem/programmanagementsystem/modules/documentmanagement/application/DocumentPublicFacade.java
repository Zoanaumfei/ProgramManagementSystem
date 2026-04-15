package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;

/**
 * Public read contract exposed by document-management to other modules.
 */
public interface DocumentPublicFacade {

    DocumentView getAccessibleDocument(String documentId, AuthenticatedUser actor);
}
