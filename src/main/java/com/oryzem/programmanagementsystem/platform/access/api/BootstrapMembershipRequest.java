package com.oryzem.programmanagementsystem.platform.access.api;

import com.oryzem.programmanagementsystem.platform.access.MembershipStatus;
import com.oryzem.programmanagementsystem.platform.authorization.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record BootstrapMembershipRequest(
        @NotBlank String organizationId,
        String marketId,
        MembershipStatus status,
        @NotEmpty Set<Role> roles) {
}
