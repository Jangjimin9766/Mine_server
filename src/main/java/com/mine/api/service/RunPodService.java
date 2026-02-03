package com.mine.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunPodService {

    private final WebClient.Builder webClientBuilder;

    @Value("${python.api.key}")
    private String apiKey;

    private static final int MAX_RETRIES = 180; // 5 seconds * 180 = 15 minutes
    private static final long RETRY_DELAY_MS = 5000;

    /**
     * RunPod Serverless Async Request (POST /run -> Poll /status/{id})
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "runPod", fallbackMethod = "fallback")
    public Map<String, Object> sendRequest(String url, Map<String, Object> inputData) {
        // 1. Convert /runsync URL to /run (if applicable)
        String runUrl = url.replace("/runsync", "/run");
        if (!runUrl.contains("/run")) {
            // If the URL doesn't contain /run, append it
            if (!runUrl.endsWith("/"))
                runUrl += "/";
            runUrl += "run";
        }

        // 2. Prepare Request
        Map<String, Object> requestBody = Map.of("input", inputData);

        // Increase buffer size for large responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        WebClient client = webClientBuilder.exchangeStrategies(strategies).build();

        // 3. Send Async Request (POST /run)
        log.info("Sending RunPod request to: {}", runUrl);
        java.util.Map<String, Object> response = client.post()
                .uri(runUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("x-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {
                })
                .block(Duration.ofSeconds(60));

        if (response == null || !response.containsKey("id")) {
            throw new RuntimeException("Failed to start RunPod job: No ID returned");
        }

        String jobId = (String) response.get("id");
        log.info("RunPod job started. ID: {}", jobId);

        // 4. Poll Status (GET /status/{id})
        // Construct status URL: replace /run with /status/{id}
        String statusUrl = runUrl.replace("/run", "/status/" + jobId);

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted");
            }

            log.debug("Polling RunPod status... ({}/{})", i + 1, MAX_RETRIES);

            java.util.Map<String, Object> statusResponse = client.get()
                    .uri(statusUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(
                            new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {
                            })
                    .block(Duration.ofSeconds(30));

            if (statusResponse == null)
                continue;

            String status = (String) statusResponse.get("status");
            log.debug("Job status: {}", status);

            if ("COMPLETED".equals(status)) {
                return statusResponse; // Contains "output"
            } else if ("FAILED".equals(status)) {
                throw new RuntimeException("RunPod job failed: " + statusResponse);
            } else if ("IN_QUEUE".equals(status) || "IN_PROGRESS".equals(status)) {
                // Wait more
                continue;
            } else {
                continue;
            }
        }

        throw new RuntimeException("RunPod job timed out after " + (MAX_RETRIES * RETRY_DELAY_MS / 1000) + " seconds");
    }

    /**
     * Local Python Server (FastAPI) Sync Request
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "runPod", fallbackMethod = "fallback")
    public Map<String, Object> sendSyncRequest(String url, Map<String, Object> requestBody) {
        log.info("Sending Sync request to: {}", url);

        // Increase buffer size for large responses (Base64 images)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        return webClientBuilder.exchangeStrategies(strategies).build()
                .post()
                .uri(url)
                .header("x-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {
                })
                .block(Duration.ofMinutes(5));
    }

    // Fallback Method
    public Map<String, Object> fallback(String url, Map<String, Object> inputData, Throwable t) {
        log.error("Circuit Breaker Open! AI Server is unreachable. URL: {}, Error: {}", url, t.getMessage());

        // Return a default failure response that Service can understand
        if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            throw new RuntimeException("AI Server is currently unavailable (Circuit Open). Please try again later.");
        }

        // For other exceptions, rethrow or return default
        throw new RuntimeException("AI Server connection failed: " + t.getMessage(), t);
    }
}
