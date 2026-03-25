package com.oryzem.programmanagementsystem.platform.access.api;

import jakarta.validation.constraints.NotBlank;

public record ActivateMembershipRequest(
        @NotBlank String membershipId,
        boolean makeDefault) {
}
