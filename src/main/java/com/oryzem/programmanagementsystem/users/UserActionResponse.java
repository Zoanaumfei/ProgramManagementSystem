package com.oryzem.programmanagementsystem.users;

import java.time.Instant;

public record UserActionResponse(
        String userId,
        String action,
        Instant performedAt,
        String status) {
}
