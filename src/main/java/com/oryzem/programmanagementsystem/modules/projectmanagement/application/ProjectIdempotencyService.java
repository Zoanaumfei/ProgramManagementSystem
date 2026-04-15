package com.oryzem.programmanagementsystem.modules.projectmanagement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.model.ProjectIdempotencyRecord;
import com.oryzem.programmanagementsystem.modules.projectmanagement.application.port.ProjectIdempotencyRepository;
import com.oryzem.programmanagementsystem.modules.projectmanagement.support.ProjectRequestHashSupport;
import com.oryzem.programmanagementsystem.platform.shared.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectIdempotencyService {

    static final int MAX_PERSISTED_PAYLOAD_CHARS = 16_000;

    private final ProjectIdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private final ProjectRequestHashSupport requestHashSupport;
    private final Clock clock;

    public ProjectIdempotencyService(
            ProjectIdempotencyRepository repository,
            ObjectMapper objectMapper,
            ProjectRequestHashSupport requestHashSupport,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.requestHashSupport = requestHashSupport;
        this.clock = clock;
    }

    @Transactional
    public <T> T execute(
            String tenantId,
            String operation,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }
        String normalizedKey = idempotencyKey.trim();
        String requestHash = requestHashSupport.hash(request);
        ProjectIdempotencyRecord existing = repository.findByIdempotencyKey(normalizedKey, tenantId, operation).orElse(null);
        if (existing != null) {
            if (!existing.requestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency-Key already exists for a different request payload.");
            }
            return deserialize(existing.responsePayload(), responseType);
        }
        T response = action.get();
        repository.save(new ProjectIdempotencyRecord(
                normalizedKey,
                tenantId,
                operation,
                requestHash,
                serialize(response),
                Instant.now(clock)));
        return response;
    }

    private String serialize(Object value) {
        try {
            String responseJson = objectMapper.writeValueAsString(value);
            ProjectIdempotencyPayload payload = new ProjectIdempotencyPayload(
                    value != null ? value.getClass().getName() : Object.class.getName(),
                    responseJson);
            String serializedPayload = objectMapper.writeValueAsString(payload);
            if (serializedPayload.length() > MAX_PERSISTED_PAYLOAD_CHARS) {
                throw new ConflictException("Idempotent response payload exceeds the persisted size limit.");
            }
            return serializedPayload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize idempotent project response.", exception);
        }
    }

    private <T> T deserialize(String payload, Class<T> responseType) {
        try {
            ProjectIdempotencyPayload storedPayload = objectMapper.readValue(payload, ProjectIdempotencyPayload.class);
            return objectMapper.readValue(storedPayload.responseJson(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize idempotent project response.", exception);
        }
    }
}
