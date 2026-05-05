package com.mine.api.service;

import com.mine.api.exception.AiServerUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunPodServiceTest {

    @Test
    @DisplayName("Circuit breaker OPEN은 503용 예외로 변환된다")
    void fallback_CallNotPermitted_ThrowsAiServerUnavailableException() {
        RunPodService service = new RunPodService(null);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("runPod");
        circuitBreaker.transitionToOpenState();
        CallNotPermittedException cause = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        AiServerUnavailableException exception = assertThrows(
                AiServerUnavailableException.class,
                () -> service.fallback(
                        "https://api.runpod.ai/v2/test/run",
                        Map.of("action", "create_moodboard"),
                        cause));

        assertEquals("AI 생성 서버가 잠시 준비 중입니다. 잠시 후 다시 시도해주세요.", exception.getMessage());
    }
}
