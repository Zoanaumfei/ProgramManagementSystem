package com.oryzem.programmanagementsystem.app.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class PingController {

    @GetMapping("/public/ping")
    public Map<String, Object> publicPing() {
        return response("public-ok");
    }

    @GetMapping("/api/ping")
    public Map<String, Object> apiPing(Authentication authentication) {
        Map<String, Object> body = response("api-ok");
        body.put("principal", authentication.getName());
        body.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.toList()));
        return body;
    }

    @GetMapping("/api/admin/ping")
    public Map<String, Object> adminPing(Authentication authentication) {
        Map<String, Object> body = response("admin-ok");
        body.put("principal", authentication.getName());
        return body;
    }

    private Map<String, Object> response(String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
