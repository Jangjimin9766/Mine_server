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

    // 5초 간격으로 최대 15분 폴링 — RunPod 콜드스타트 + AI 처리 시간 카버
    private static final int MAX_RETRIES = 180; // 5 seconds * 180 = 15 minutes
    private static final long RETRY_DELAY_MS = 5000;

    /**
     * RunPod Serverless Async Request (POST /run -> Poll /status/{id})
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "runPod", fallbackMethod = "fallback")
    public Map<String, Object> sendRequest(String url, Map<String, Object> inputData) {
        // RunPod Serverless는 POST /run 요청 후 작업 ID를 받아 폴링하는 비동기 구조
        String runUrl = url.replace("/runsync", "/run");
        if (!runUrl.contains("/run")) {
            if (!runUrl.endsWith("/"))
                runUrl += "/";
            runUrl += "run";
        }

        // input 필드로 래핑되어야 RunPod이 인식함
        Map<String, Object> requestBody = Map.of("input", inputData);

        // Base64 이미지 응답 등 대용량 응답을 위해 16MB로 확장
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
        // WebClient.Builder는 공용 Bean이므로 .clone()을 사용하여 부수 효과 방지 (StackOverflowError 픽스)
        WebClient client = webClientBuilder.clone().exchangeStrategies(strategies).build();

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
                .block(Duration.ofSeconds(120));

        if (response == null || !response.containsKey("id")) {
            throw new RuntimeException("Failed to start RunPod job: No ID returned");
        }

        String jobId = (String) response.get("id");
        log.info("RunPod job started. ID: {}", jobId);

        // 4. Poll Status (GET /status/{id})
        // Construct status URL: replace /run with /status/{id}
        String statusUrl = runUrl.replace("/run", "/status/" + jobId);
        log.info("Starting RunPod polling at: {}", statusUrl);

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
                log.info("RunPod job COMPLETED. Extracting output.");
                return statusResponse; // output 필드에 AI 결과가 담겨 있다
            } else if ("FAILED".equals(status)) {
                log.error("RunPod job FAILED. Full response: {}", statusResponse);
                throw new RuntimeException("RunPod job failed: " + statusResponse);
            } else if ("IN_QUEUE".equals(status) || "IN_PROGRESS".equals(status)) {
                // 작업 대기 중 — 다음 폴링 시도
                continue;
            } else {
                continue;
            }
        }

        throw new RuntimeException("RunPod job timed out after " + (MAX_RETRIES * RETRY_DELAY_MS / 1000) + " seconds");
    }

    // 로컴 FastAPI 서버 전용 동기 방식 — RunPod와 달리 input 래핑 및 폴링 불필요
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "runPod", fallbackMethod = "fallback")
    public Map<String, Object> sendSyncRequest(String url, Map<String, Object> requestBody) {
        log.info("Sending Sync request to: {}", url);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
        // WebClient.Builder는 공용 Bean이므로 .clone() 사용 시 메모리 누수 방지
        return webClientBuilder.clone().exchangeStrategies(strategies).build()
                .post()
                .uri(url)
                .header("x-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {
                })
                .block(Duration.ofMinutes(5)); // 로컀 AI 요청은 최대 5분 대기
    }

    public Map<String, Object> fallback(String url, Map<String, Object> inputData, Throwable t) {
        String errorMessage = t.getMessage();
        if (errorMessage == null) {
            errorMessage = t.getClass().getSimpleName() + " (No detailed message)";
        }
        
        log.error("Circuit Breaker Open! AI Server is unreachable. URL: {}, Error: {}", url, errorMessage);
        log.error("Detailed Exception in RunPodService:", t);

        // Return a default failure response that Service can understand
        if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            throw new RuntimeException("AI Server is currently unavailable (Circuit Open). Please try again later.", t);
        }

        // For other exceptions, rethrow or return default
        throw new RuntimeException("AI Server connection failed: " + errorMessage, t);
    }
}
