package com.oryzem.programmanagementsystem.operations;

import jakarta.validation.constraints.NotBlank;

public record UpdateOperationRequest(
        @NotBlank String title,
        String description) {
}
