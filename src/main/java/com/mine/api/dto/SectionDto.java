package com.mine.api.dto;

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
        private Long id;

        @Schema(description = "소제목", example = "서울의 숨겨진 카페")
        private String heading;

        @Schema(description = "본문 내용", example = "이곳은 조용히 커피를 즐기기 좋은 공간입니다.")
        private String content;

        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        private String imageUrl;

        @Schema(description = "레이아웃 타입", example = "card")
        private String layoutType;

        @Schema(description = "레이아웃 힌트", example = "full_width")
        private String layoutHint;

        @Schema(description = "이미지 캡션", example = "카페 내부 전경")
        private String caption;

        @Schema(description = "표시 순서", example = "1")
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
    @Schema(description = "섹션 직접 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "수정할 소제목", example = "체크 패턴의 매력")
        private String heading;

        @Schema(description = "수정할 본문 (HTML)", example = "<p>요즘 체크무늬를...</p>")
        private String content;

        @Schema(description = "수정할 이미지 URL", example = "https://example.com/image.jpg")
        private String imageUrl;

        @Schema(description = "수정할 캡션", example = "체크 패턴 스타일링")
        private String caption;
    }
}
