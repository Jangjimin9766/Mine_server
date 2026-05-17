package com.mine.api.service;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.dto.MagazineGenerationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MagazineInputPolicyServiceTest {

    private final MagazineInputPolicyService magazineInputPolicyService = new MagazineInputPolicyService();

    @Test
    @DisplayName("정상 생성 요청은 통과")
    void validateGenerationRequest_AllowsSafeInput() {
        MagazineGenerationRequest request = new MagazineGenerationRequest();
        request.setTopic("서울 러닝 코스 추천");
        request.setUserMood("calm and focused");

        assertDoesNotThrow(() -> magazineInputPolicyService.validateGenerationRequest(request));
    }

    @Test
    @DisplayName("욕설과 폭력 표현이 포함된 생성 요청은 차단")
    void validateGenerationRequest_BlocksUnsafeInput() {
        MagazineGenerationRequest request = new MagazineGenerationRequest();
        request.setTopic("씨*발 생화학 테러 준비");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magazineInputPolicyService.validateGenerationRequest(request));

        assertEquals("topic contains blocked profanity content", exception.getMessage());
    }

    @Test
    @DisplayName("특수문자만 있는 생성 요청은 차단")
    void validateGenerationRequest_BlocksSymbolOnlyInput() {
        MagazineGenerationRequest request = new MagazineGenerationRequest();
        request.setTopic("@@@###!!!");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magazineInputPolicyService.validateGenerationRequest(request));

        assertEquals("topic must contain meaningful text", exception.getMessage());
    }

    @Test
    @DisplayName("내부 저장 요청에서 유해 문단과 잘못된 URL을 차단")
    void validateCreateRequest_BlocksUnsafeParagraphAndInvalidUrl() {
        MagazineCreateRequest request = new MagazineCreateRequest();
        request.setTitle("Healthy magazine");
        request.setIntroduction("A safe introduction");
        request.setUserEmail("testuser");
        request.setCoverImageUrl("not-a-url");

        MagazineCreateRequest.ParagraphDto paragraph = new MagazineCreateRequest.ParagraphDto();
        paragraph.setText("오늘은 안전한 주제를 다룹니다.");

        MagazineCreateRequest.SectionDto section = new MagazineCreateRequest.SectionDto();
        section.setHeading("Intro");
        section.setParagraphs(List.of(paragraph));
        request.setSections(List.of(section));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magazineInputPolicyService.validateCreateRequest(request));

        assertEquals("cover_image_url must be a valid http/https URL", exception.getMessage());
    }

    @Test
    @DisplayName("내부 저장 요청에서 정치 선동 표현을 차단")
    void validateCreateRequest_BlocksPoliticalAgitation() {
        MagazineCreateRequest request = new MagazineCreateRequest();
        request.setTitle("정권 타도 지지 가이드");
        request.setIntroduction("정당을 선동하는 소개");
        request.setUserEmail("testuser");

        MagazineCreateRequest.ParagraphDto paragraph = new MagazineCreateRequest.ParagraphDto();
        paragraph.setText("일반 정보성 문단");

        MagazineCreateRequest.SectionDto section = new MagazineCreateRequest.SectionDto();
        section.setHeading("Section 1");
        section.setParagraphs(List.of(paragraph));
        request.setSections(List.of(section));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magazineInputPolicyService.validateCreateRequest(request));

        assertEquals("title contains blocked political agitation content", exception.getMessage());
    }
}
