package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 섹션 관련 DTO
 */
public class SectionDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "섹션 상세 정보")
    public static class Response {
        @Schema(description = "섹션 ID", example = "101")
        @JsonProperty("sectionId")
        private Long id;

        @Schema(description = "소제목", example = "서울의 숨겨진 카페")
        private String heading;

        // ===== 새로 추가된 필드 =====

        @Schema(description = "섹션 썸네일 URL", example = "https://example.com/thumbnail.jpg")
        @JsonProperty("thumbnailUrl")
        private String thumbnailUrl;

        @Schema(description = "문단 배열 (지그재그 레이아웃용)")
        private List<ParagraphDto.Response> paragraphs;

        // ===== 기존 필드 =====

        @Schema(description = "레이아웃 타입", example = "card")
        @JsonProperty("layoutType")
        private String layoutType;

        @Schema(description = "레이아웃 힌트", example = "full_width")
        @JsonProperty("layoutHint")
        private String layoutHint;

        @Schema(description = "표시 순서", example = "1")
        @JsonProperty("displayOrder")
        private Integer displayOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "섹션 순서 변경 요청")
    public static class ReorderRequest {
        @NotNull(message = "섹션 ID 목록은 필수입니다")
        @Schema(description = "새 순서대로 정렬된 섹션 ID 목록", example = "[101, 103, 102]")
        private List<Long> sectionIds; // 새 순서대로 정렬된 섹션 ID 목록
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 섹션 수정 요청")
    public static class InteractRequest {
        @NotNull(message = "메시지는 필수입니다")
        @Schema(description = "AI에게 내릴 명령 (프롬프트)", example = "이 내용을 좀 더 감성적으로 바꿔줘")
        private String message; // 사용자 프롬프트
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractResponse {
        private String message;
        private String actionType; // "regenerate_content", "edit_content", "change_tone"
        private Long sectionId;
        private Response section;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SectionUpdateRequest", description = "섹션 직접 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "수정할 소제목", example = "체크 패턴의 매력")
        private String heading;

        @Schema(description = "수정할 썸네일 URL", example = "https://example.com/new-thumb.jpg")
        private String thumbnailUrl;

        @Schema(description = "수정할 레이아웃 타입", example = "card")
        private String layoutType;

        @Schema(description = "수정할 레이아웃 힌트", example = "full_width")
        private String layoutHint;

        @Schema(description = "수정할 문단 목록")
        private List<ParagraphUpdateRequest> paragraphs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ParagraphUpdateRequest", description = "문단 수정 요청")
    public static class ParagraphUpdateRequest {
        @Schema(description = "소제목", example = "봄의 시작")
        private String subtitle;

        @Schema(description = "본문", example = "봄이 오면 꽃이 핀다.")
        private String text;

        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        private String imageUrl;
    }
}
