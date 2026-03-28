package com.oryzem.programmanagementsystem.platform.access;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.multitenancy.rate-limit", name = "store", havingValue = "redis", matchIfMissing = true)
class RedisTenantRateLimitCounterStore implements TenantRateLimitCounterStore {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_WINDOW_TTL = new DefaultRedisScript<>(
            """
                    local current = redis.call('INCR', KEYS[1])
                    if current == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    return current
                    """,
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final TenantGovernanceProperties properties;

    RedisTenantRateLimitCounterStore(StringRedisTemplate stringRedisTemplate, TenantGovernanceProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public long increment(String tenantId, Duration window) {
        String key = properties.getRateLimit().getRedisKeyPrefix() + ":" + tenantId;
        Long value = stringRedisTemplate.execute(
                INCREMENT_WITH_WINDOW_TTL,
                java.util.List.of(key),
                String.valueOf(window.toMillis()));
        if (value == null) {
            throw new IllegalStateException("Unable to increment tenant rate limit counter.");
        }
        return value;
    }
}
