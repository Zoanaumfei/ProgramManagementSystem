package com.oryzem.programmanagementsystem.platform.audit;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequestCorrelationContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public String get() {
        return CURRENT.get();
    }

    public String getOrCreate() {
        String correlationId = CURRENT.get();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            CURRENT.set(correlationId);
        }
        return correlationId;
    }

    public void clear() {
        CURRENT.remove();
    }
}
