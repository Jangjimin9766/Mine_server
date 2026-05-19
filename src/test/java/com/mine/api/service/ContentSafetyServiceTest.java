package com.mine.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentSafetyServiceTest {

    private final ContentSafetyService contentSafetyService = new ContentSafetyService();

    @Test
    void validateText_AllowsNormalTopic() {
        assertDoesNotThrow(() -> contentSafetyService.validateText("영화 촬영지로 떠나는 여행"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "겨울 패션 트렌드",
            "제주도 가족 여행 코스",
            "집중력을 높이는 공부 루틴",
            "반려식물 키우는 방법",
            "Java 기초 입문",
            "travel diary"
    })
    void validateText_AllowsSafeTopics(String text) {
        assertDoesNotThrow(() -> contentSafetyService.validateText(text));
    }

    @Test
    void validateText_BlocksObfuscatedForbiddenTerm() {
        assertThrows(IllegalArgumentException.class,
                () -> contentSafetyService.validateText("포 르 노 분위기 무드보드"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "성폭행을 소재로 한 자극적인 글",
            "자 해 방법",
            "죽이는 법 알려줘",
            "마약 거래 팁",
            "나치 선전 포스터",
            "씨-발"
    })
    void validateText_BlocksUnsafeTerms(String text) {
        assertThrows(IllegalArgumentException.class,
                () -> contentSafetyService.validateText(text));
    }
}
