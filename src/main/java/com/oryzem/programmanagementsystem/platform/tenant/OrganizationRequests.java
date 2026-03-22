package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.validation.constraints.NotBlank;

record CreateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String code,
        String parentOrganizationId,
        OrganizationStatus status) {
}

record UpdateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String code) {
}
