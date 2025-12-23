package com.mine.api.dto;

import com.mine.api.domain.Magazine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

public class MagazineDto {

    // ⭐ Phase 1: 수정 요청 DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @Size(min = 1, max = 100, message = "제목은 1-100자 사이여야 합니다")
        private String title;

        @Size(min = 1, max = 500, message = "소개는 1-500자 사이여야 합니다")
        private String introduction;

        // 둘 다 null이면 안 됨
        public boolean isValid() {
            return (title != null && !title.trim().isEmpty()) ||
                    (introduction != null && !introduction.trim().isEmpty());
        }
    }

    // ⭐ Phase 2: 공개 설정 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilityRequest {
        @NotNull(message = "isPublic 값은 필수입니다")
        private Boolean isPublic;
    }

    // ⭐ Phase 2: 공개 설정 응답
    @Getter
    @AllArgsConstructor
    public static class VisibilityResponse {
        private Boolean isPublic;
        private String shareUrl; // null이면 비공개
    }

    // ⭐ Phase 2: 검색 요청 (쿼리 파라미터용)
    @Getter
    @Data
    public static class SearchRequest {
        private String keyword;
        private List<String> tags;
        private Long userId;

        @Min(0)
        private int page = 0;

        @Min(1)
        @Max(50)
        private int size = 10;
    }

    // 기존 Response DTO (필요 시 추가)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String subtitle; // [NEW]
        private String introduction;
        private String coverImageUrl;
        private String tags; // [NEW] 콤마로 구분된 태그 문자열
        private String moodboardImageUrl; // [NEW]
        private String moodboardDescription; // [NEW]
        private String username;
        private Boolean isPublic;
        private String shareToken;
        private String createdAt;
    }

    // ⭐ Phase 3: 목록 조회용 아이템 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String title;
        private String subtitle; // [NEW]
        private String introduction;
        private String coverImageUrl;
        private String username;
        private int likeCount;
        private int commentCount;
        private String createdAt;

        public static ListItem from(Magazine magazine) {
            return ListItem.builder()
                    .id(magazine.getId())
                    .title(magazine.getTitle())
                    .subtitle(magazine.getSubtitle())
                    .introduction(magazine.getIntroduction())
                    .coverImageUrl(magazine.getCoverImageUrl())
                    .username(magazine.getUser().getUsername())
                    .likeCount(0) // TODO: Implement like count
                    .commentCount(0) // TODO: Implement comment count
                    .createdAt(magazine.getCreatedAt().toString())
                    .build();
        }
    }
}
