package com.oryzem.programmanagementsystem.platform.access;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRateLimitingFilterTest {

    @Mock
    private AuthenticatedUserMapper authenticatedUserMapper;

    @Mock
    private TenantGovernanceService tenantGovernanceService;

    @Mock
    private RequestCorrelationContext requestCorrelationContext;

    private final LocalTenantRateLimitCounterStore counterStore = new LocalTenantRateLimitCounterStore();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<AuthenticatedUser> currentActor = new AtomicReference<>();

    private TenantRateLimitingFilter filterInstanceA;
    private TenantRateLimitingFilter filterInstanceB;

    @BeforeEach
    void setUp() {
        counterStore.clear();
        SecurityContextHolder.clearContext();

        filterInstanceA = new TenantRateLimitingFilter(
                authenticatedUserMapper,
                tenantGovernanceService,
                counterStore,
                requestCorrelationContext,
                objectMapper);
        filterInstanceB = new TenantRateLimitingFilter(
                authenticatedUserMapper,
                tenantGovernanceService,
                counterStore,
                requestCorrelationContext,
                objectMapper);

        when(authenticatedUserMapper.from(any(Authentication.class))).thenAnswer(invocation -> currentActor.get());
        when(requestCorrelationContext.getOrCreate()).thenReturn("corr-rate-limit-test");
        lenient().when(tenantGovernanceService.rateLimitPolicy("TEN-tenant-a"))
                .thenReturn(new TenantGovernanceService.RateLimitPolicy(Duration.ofSeconds(60), 2));
        lenient().when(tenantGovernanceService.rateLimitPolicy("TEN-tenant-b"))
                .thenReturn(new TenantGovernanceService.RateLimitPolicy(Duration.ofSeconds(60), 2));
    }

    @Test
    void shouldShareCountersAcrossInstancesAndReturnStable429Payload() throws Exception {
        currentActor.set(actor("TEN-tenant-a"));
        SecurityContextHolder.getContext().setAuthentication(authentication());

        assertAllowed(filterInstanceA);
        assertAllowed(filterInstanceB);

        MockHttpServletResponse throttled = perform(filterInstanceA);

        assertThat(throttled.getStatus()).isEqualTo(429);
        assertThat(throttled.getContentAsString()).contains("\"status\":429");
        assertThat(throttled.getContentAsString()).contains("\"path\":\"/api/auth/me\"");
        assertThat(throttled.getContentAsString()).contains("\"correlationId\":\"corr-rate-limit-test\"");
        assertThat(throttled.getContentAsString()).contains("Tenant rate limit exceeded.");
    }

    @Test
    void shouldIsolateCountersPerTenant() throws Exception {
        currentActor.set(actor("TEN-tenant-a"));
        SecurityContextHolder.getContext().setAuthentication(authentication());

        assertAllowed(filterInstanceA);
        assertAllowed(filterInstanceB);
        perform(filterInstanceA);

        currentActor.set(actor("TEN-tenant-b"));
        SecurityContextHolder.getContext().setAuthentication(authentication());
        MockHttpServletResponse tenantBResponse = perform(filterInstanceB);

        assertThat(tenantBResponse.getStatus()).isEqualTo(200);
    }

    private void assertAllowed(TenantRateLimitingFilter filter) throws Exception {
        MockHttpServletResponse response = perform(filter);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse perform(TenantRateLimitingFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private Authentication authentication() {
        return new TestingAuthenticationToken("principal", "credentials", "ROLE_ADMIN");
    }

    private AuthenticatedUser actor(String tenantId) {
        return new AuthenticatedUser(
                "subject",
                "admin@example.com",
                Set.of(Role.ADMIN),
                Set.of(),
                "USR-ADMIN-001",
                "MBR-ADMIN-001",
                tenantId,
                tenantId.substring(tenantId.indexOf('-') + 1),
                null,
                TenantType.EXTERNAL);
    }
}
