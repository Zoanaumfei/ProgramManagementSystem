package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

public record ProjectIdempotencyPayload(
        String responseType,
        String responseJson) {
}
