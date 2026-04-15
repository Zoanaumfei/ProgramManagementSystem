package com.oryzem.programmanagementsystem.modules.documentmanagement.contract;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class DocumentContextPolicyProviderContractTest {

    protected abstract DocumentContextPolicyProvider provider();

    protected abstract String existingContextId();

    protected abstract String missingContextId();

    protected AuthenticatedUser actor() {
        return new AuthenticatedUser(
                "sub",
                "admin.a@tenant.com",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-1",
                "MBR-1",
                "TEN-tenant-a",
                "tenant-a",
                null,
                TenantType.EXTERNAL);
    }

    @Test
    void shouldAdvertiseSupportedContextType() {
        assertThat(provider().supports()).isEqualTo(DocumentContextType.PROJECT);
    }

    @Test
    void shouldReturnExistingContextWithOwnerOrganization() {
        DocumentContextPolicy policy = provider().resolve(existingContextId(), actor());

        assertThat(policy.exists()).isTrue();
        assertThat(policy.ownerOrganizationId()).isNotBlank();
        assertThat(policy.featureEnabled()).isTrue();
    }

    @Test
    void shouldReturnMissingContextWithoutPermissions() {
        DocumentContextPolicy policy = provider().resolve(missingContextId(), actor());

        assertThat(policy.exists()).isFalse();
        assertThat(policy.canViewDocuments()).isFalse();
        assertThat(policy.canUploadDocuments()).isFalse();
        assertThat(policy.canDownloadDocuments()).isFalse();
        assertThat(policy.canDeleteDocuments()).isFalse();
    }
}
