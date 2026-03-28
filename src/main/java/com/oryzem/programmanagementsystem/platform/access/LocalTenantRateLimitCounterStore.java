package com.oryzem.programmanagementsystem.platform.access;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.multitenancy.rate-limit", name = "store", havingValue = "local")
class LocalTenantRateLimitCounterStore implements TenantRateLimitCounterStore {

    private final ConcurrentHashMap<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public long increment(String tenantId, Duration window) {
        long windowMillis = window.toMillis();
        long now = System.currentTimeMillis();
        FixedWindowCounter counter = counters.compute(tenantId, (key, current) -> {
            if (current == null || now >= current.windowEndsAtEpochMillis) {
                return new FixedWindowCounter(now + windowMillis);
            }
            return current;
        });
        return counter.requests.incrementAndGet();
    }

    @Override
    public void clear() {
        counters.clear();
    }

    private static final class FixedWindowCounter {
        private final long windowEndsAtEpochMillis;
        private final AtomicInteger requests = new AtomicInteger();

        private FixedWindowCounter(long windowEndsAtEpochMillis) {
            this.windowEndsAtEpochMillis = windowEndsAtEpochMillis;
        }
    }
}
