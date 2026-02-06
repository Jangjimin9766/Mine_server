package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 섹션 열람 기록 DTO
 */
public class SectionViewHistoryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최근 열람 섹션 정보")
    public static class Response {
        @Schema(description = "섹션 ID", example = "223")
        private Long id;

        @Schema(description = "소제목", example = "3주 유럽여행 코스 추천")
        private String heading;

        @Schema(description = "본문 내용", example = "<p>첫 유럽여행을 계획하는 분들에게 추천하는 3주 일정입니다.</p>")
        private String content;

        @Schema(description = "이미지 URL", example = "https://example.com/europe.jpg")
        private String imageUrl;

        @Schema(description = "레이아웃 타입", example = "hero")
        private String layoutType;

        @Schema(description = "레이아웃 힌트", example = "image_left")
        private String layoutHint;

        @Schema(description = "이미지 캡션", example = "유럽의 아름다움, 여행의 시작")
        private String caption;

        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;

        @Schema(description = "매거진 ID", example = "97")
        private Long magazineId;

        @Schema(description = "매거진 제목", example = "유럽여행: 완벽한 3주 일정 안내")
        private String magazineTitle;

        @Schema(description = "열람 시각", example = "2026-01-31T09:55:38")
        private LocalDateTime viewedAt;
    }
}
