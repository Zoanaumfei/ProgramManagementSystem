package com.oryzem.programmanagementsystem.platform.tenant;

import jakarta.validation.constraints.NotBlank;

record OrganizationExportRequest(@NotBlank String justification) {
}
