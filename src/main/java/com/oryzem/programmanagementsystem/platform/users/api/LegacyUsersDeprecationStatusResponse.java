package com.oryzem.programmanagementsystem.platform.users.api;

import com.oryzem.programmanagementsystem.platform.users.deprecation.LegacyUsersDeprecationStage;
import java.time.Instant;

public record LegacyUsersDeprecationStatusResponse(
        boolean usersLegacyUiEnabled,
        boolean usersLegacyReadEnabled,
        boolean usersLegacyWriteEnabled,
        LegacyUsersDeprecationStage currentStage,
        String legacyPath,
        String replacementPath,
        Instant generatedAt) {
}
