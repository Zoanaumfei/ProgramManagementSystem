package com.oryzem.programmanagementsystem.platform.shared;

import java.util.Collections;
import java.util.Map;

public class BusinessRuleException extends IllegalArgumentException {

    private final String code;
    private final Map<String, Object> details;

    public BusinessRuleException(String code, String message) {
        this(code, message, Map.of());
    }

    public BusinessRuleException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
