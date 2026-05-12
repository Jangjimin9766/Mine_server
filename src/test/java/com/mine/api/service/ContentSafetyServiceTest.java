package com.mine.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentSafetyServiceTest {

    private final ContentSafetyService contentSafetyService = new ContentSafetyService();

    @Test
    void validateText_AllowsNormalTopic() {
        assertDoesNotThrow(() -> contentSafetyService.validateText("영화 촬영지로 떠나는 여행"));
    }

    @Test
    void validateText_BlocksObfuscatedForbiddenTerm() {
        assertThrows(IllegalArgumentException.class,
                () -> contentSafetyService.validateText("포 르 노 분위기 무드보드"));
    }
}
