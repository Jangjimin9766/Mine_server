package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문단 관련 DTO
 * 각 문단은 subtitle, text, imageUrl 세트로 구성
 */
public class ParagraphDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "문단 상세 정보")
    public static class Response {
        @Schema(description = "문단 ID", example = "10")
        @JsonProperty("paragraphId")
        private Long id;

        @Schema(description = "문단 소제목", example = "국밥의 성지, 서면")
        private String subtitle;

        @Schema(description = "문단 본문 (150-300자)", example = "돼지국밥은 부산의 대표 음식으로, 진한 국물과 부드러운 돼지고기가 특징입니다.")
        private String text;

        @Schema(description = "문단 이미지 URL", example = "https://example.com/pork_soup.jpg")
        @JsonProperty("imageUrl")
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "문단 생성 요청")
    public static class CreateRequest {
        @Schema(description = "문단 소제목", example = "새로운 문단 제목")
        private String subtitle;

        @Schema(description = "문단 본문 (150-300자)", example = "추가된 문단의 내용입니다...")
        private String text;

        @Schema(description = "문단 이미지 URL", example = "https://example.com/new_image.jpg")
        @JsonProperty("imageUrl")
        private String imageUrl;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 문단 생성 요청")
    public static class AiCreateRequest {
        @Schema(description = "AI에게 전달할 메시지 (주제, 지시사항 등)", example = "해운대 맛집에 대한 내용을 추가해줘")
        private String message;
    }
}

