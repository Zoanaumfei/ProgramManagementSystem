package com.oryzem.programmanagementsystem.modules.documentmanagement.domain;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;

public interface DocumentContextPolicyProvider {

    DocumentContextType supports();

    DocumentContextPolicy resolve(String contextId, AuthenticatedUser actor);
}
