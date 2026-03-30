package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.validation.constraints.NotBlank;

record CreateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String code,
        OrganizationStatus status) {
}

record UpdateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String code) {
}
