package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitoUserIdentityGatewayTest {

    @Test
    void createUserShouldTranslateDuplicateCognitoUsernameToConflict() {
        CognitoIdentityProviderClient client = Mockito.mock(CognitoIdentityProviderClient.class);
        Mockito.when(client.adminCreateUser(Mockito.any(AdminCreateUserRequest.class)))
                .thenThrow(UsernameExistsException.builder().message("User account already exists").build());

        CognitoUserIdentityGateway gateway = new CognitoUserIdentityGateway(
                new UserIdentityProperties("cognito", "pool-id", "sa-east-1"),
                client);

        ManagedUser user = new ManagedUser(
                "USR-123",
                "existing.user@tenant.com",
                null,
                "Existing User",
                "existing.user@tenant.com",
                UserStatus.INVITED,
                Instant.parse("2026-03-30T00:00:00Z"),
                null,
                null);

        assertThatThrownBy(() -> gateway.createUser(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists in Cognito");
    }
}
