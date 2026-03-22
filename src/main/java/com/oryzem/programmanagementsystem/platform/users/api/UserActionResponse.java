package com.oryzem.programmanagementsystem.platform.users.api;

import java.time.Instant;

public record UserActionResponse(
        String userId,
        String action,
        Instant performedAt,
        String status) {
}
