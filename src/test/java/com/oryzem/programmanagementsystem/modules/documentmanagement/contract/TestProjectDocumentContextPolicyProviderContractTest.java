package com.oryzem.programmanagementsystem.modules.documentmanagement.contract;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.support.TestProjectDocumentContextPolicyProvider;

class TestProjectDocumentContextPolicyProviderContractTest extends DocumentContextPolicyProviderContractTest {

    private final TestProjectDocumentContextPolicyProvider provider = new TestProjectDocumentContextPolicyProvider();

    @Override
    protected DocumentContextPolicyProvider provider() {
        return provider;
    }

    @Override
    protected String existingContextId() {
        return "project-123";
    }

    @Override
    protected String missingContextId() {
        return "project-missing";
    }
}
