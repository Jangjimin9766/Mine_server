package com.mine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 섹션 관련 DTO
 */
public class SectionDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String heading;
        private String content;
        private String imageUrl;
        private String layoutType;
        private String layoutHint;
        private String caption;
        private Integer displayOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderRequest {
        @NotNull(message = "섹션 ID 목록은 필수입니다")
        private List<Long> sectionIds; // 새 순서대로 정렬된 섹션 ID 목록
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractRequest {
        @NotNull(message = "메시지는 필수입니다")
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
}
