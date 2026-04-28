package com.mine.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @Value("${python.api.url:}")
    private String pythonApiUrl;

    @Value("${python.api.moodboard-url:${mine.internal.moodboard-url:}}")
    private String moodboardApiUrl;

    @Value("${python.api.key:}")
    private String pythonApiKey;

    @Value("${mine.internal.secret-key:}")
    private String internalSecretKey;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "config", Map.of(
                        "pythonApiUrlSet", pythonApiUrl != null && !pythonApiUrl.isBlank(),
                        "moodboardApiUrlSet", moodboardApiUrl != null && !moodboardApiUrl.isBlank(),
                        "pythonApiKeySet", pythonApiKey != null && !pythonApiKey.isBlank(),
                        "internalSecretKeySet", internalSecretKey != null && !internalSecretKey.isBlank()
                )
        ));
    }
}
