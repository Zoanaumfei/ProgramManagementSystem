package com.oryzem.programmanagementsystem.platform.access;

import java.time.Duration;

public interface TenantRateLimitCounterStore {

    long increment(String tenantId, Duration window);

    default void clear() {
    }
}
