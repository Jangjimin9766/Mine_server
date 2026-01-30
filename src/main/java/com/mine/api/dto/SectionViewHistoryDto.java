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
        @Schema(description = "섹션 ID", example = "15")
        private Long id;

        @Schema(description = "소제목", example = "글래스모피즘의 부활")
        private String heading;

        @Schema(description = "본문 내용")
        private String content;

        @Schema(description = "이미지 URL")
        private String imageUrl;

        @Schema(description = "레이아웃 타입", example = "split_left")
        private String layoutType;

        @Schema(description = "레이아웃 힌트", example = "full_width")
        private String layoutHint;

        @Schema(description = "이미지 캡션")
        private String caption;

        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;

        @Schema(description = "매거진 ID", example = "90")
        private Long magazineId;

        @Schema(description = "매거진 제목", example = "2026 디자인 트렌드")
        private String magazineTitle;

        @Schema(description = "열람 시각")
        private LocalDateTime viewedAt;
    }
}
