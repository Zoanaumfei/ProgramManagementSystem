package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.validation.constraints.NotBlank;

record CreateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String cnpj,
        OrganizationStatus status,
        String localOrganizationCode) {
}

record UpdateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String cnpj) {
}
