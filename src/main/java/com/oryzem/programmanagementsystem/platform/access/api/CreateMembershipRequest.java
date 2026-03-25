package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.MembershipStatus;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CreateMembershipRequest(
        @NotBlank String tenantId,
        String organizationId,
        String marketId,
        MembershipStatus status,
        boolean defaultMembership,
        @NotEmpty Set<Role> roles) {
}
