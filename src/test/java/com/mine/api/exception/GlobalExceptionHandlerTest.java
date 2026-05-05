package com.mine.api.exception;

import com.mine.api.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("AI 서버 준비 중 예외는 503 응답으로 변환된다")
    void handleAiServerUnavailableException_Returns503() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleAiServerUnavailableException(
                new AiServerUnavailableException("AI 생성 서버가 잠시 준비 중입니다. 잠시 후 다시 시도해주세요."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("AI 생성 서버가 잠시 준비 중입니다. 잠시 후 다시 시도해주세요.", response.getBody().getMessage());
    }
}
