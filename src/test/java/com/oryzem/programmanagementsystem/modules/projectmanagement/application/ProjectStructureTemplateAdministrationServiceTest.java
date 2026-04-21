package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationLookup;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

class ProjectStructureTemplateAdministrationServiceTest {

    private final OrganizationLookup organizationLookup = Mockito.mock(OrganizationLookup.class);
    private final ProjectStructureTemplateAdministrationService service =
            new ProjectStructureTemplateAdministrationService(organizationLookup);

    @Test
    void shouldAuthorizeUseForOwnerAndDescendant() {
        AuthenticatedUser owner = actor("ORG-OWNER", Role.ADMIN);
        AuthenticatedUser descendant = actor("ORG-DESC", Role.MEMBER);

        when(organizationLookup.isSameOrDescendant("ORG-OWNER", "ORG-DESC")).thenReturn(true);

        assertThat(service.canUse(owner, "ORG-OWNER")).isTrue();
        assertThat(service.canUse(descendant, "ORG-OWNER")).isTrue();
    }

    @Test
    void shouldDenyManagementForNonOwnerEvenIfAdmin() {
        AuthenticatedUser nonOwnerAdmin = actor("ORG-OTHER", Role.ADMIN);

        assertThatThrownBy(() -> service.authorizeManagement(nonOwnerAdmin, "ORG-OWNER"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRequireAdminAndOrganizationContextForTemplateCreation() {
        AuthenticatedUser member = actor("ORG-OWNER", Role.MEMBER);
        AuthenticatedUser adminWithoutOrg = new AuthenticatedUser(
                "sub-admin",
                "admin-without-org",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-ADMIN",
                "MBR-ADMIN",
                "tenant-a",
                null,
                null,
                TenantType.EXTERNAL);

        assertThatThrownBy(() -> service.authorizeTemplateCreation(member))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.authorizeTemplateCreation(adminWithoutOrg))
                .isInstanceOf(AccessDeniedException.class);
    }

    private AuthenticatedUser actor(String organizationId, Role role) {
        return new AuthenticatedUser(
                "sub-" + organizationId + "-" + role.name(),
                "user-" + organizationId + "-" + role.name(),
                Set.of(role),
                Set.of(),
                "USR-" + organizationId + "-" + role.name(),
                "MBR-" + organizationId + "-" + role.name(),
                "tenant-a",
                organizationId,
                null,
                TenantType.EXTERNAL);
    }
}
