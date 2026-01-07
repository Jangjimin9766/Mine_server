package com.mine.api.dto;

import com.mine.api.domain.Magazine;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "매거진 수정 요청")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @Schema(description = "✏️ 수정할 제목 (1-100자)", example = "2024 F/W 패션 트렌드 (수정됨)")
        @Size(min = 1, max = 100, message = "제목은 1-100자 사이여야 합니다")
        private String title;

        @Schema(description = "📝 수정할 소개 (1-500자)", example = "이번 시즌 놓쳐선 안 될 스타일링 팁을 모았습니다.")
        @Size(min = 1, max = 500, message = "소개는 1-500자 사이여야 합니다")
        private String introduction;

        public boolean isValid() {
            return (title != null && !title.trim().isEmpty()) ||
                    (introduction != null && !introduction.trim().isEmpty());
        }
    }

    @Schema(description = "공개/비공개 설정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilityRequest {
        @Schema(description = "공개 여부 (true: 공개, false: 비공개)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isPublic 값은 필수입니다")
        private Boolean isPublic;
    }

    @Schema(description = "공개/비공개 설정 응답")
    @Getter
    @AllArgsConstructor
    public static class VisibilityResponse {
        @Schema(description = "공개 여부", example = "true")
        private Boolean isPublic;

        @Schema(description = "공유 URL (공개 시에만 제공)", example = "http://localhost:3000/share/abc123xyz")
        private String shareUrl;
    }

    @Schema(description = "매거진 검색 요청")
    @Getter
    @Data
    public static class SearchRequest {
        @Schema(description = "검색 키워드", example = "패션")
        private String keyword;

        @Schema(description = "필터링할 태그 목록", example = "[\"패션\", \"겨울\"]")
        private List<String> tags;

        @Schema(description = "특정 사용자 ID로 필터링")
        private Long userId;

        @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
        @Min(0)
        private int page = 0;

        @Schema(description = "페이지 크기 (1-50)", example = "10")
        @Min(1)
        @Max(50)
        private int size = 10;
    }

    @Schema(description = "매거진 상세 응답")
    @Getter
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "매거진 ID", example = "1")
        private Long id;

        @Schema(description = "매거진 제목", example = "겨울철 패션 트렌드")
        private String title;

        @Schema(description = "매거진 부제", example = "따뜻함과 스타일을 동시에")
        private String subtitle;

        @Schema(description = "매거진 소개", example = "올 겨울 핫한 스타일링 가이드")
        private String introduction;

        @Schema(description = "커버 이미지 URL", example = "https://example.com/cover.jpg")
        private String coverImageUrl;

        @Schema(description = "태그 (콤마로 구분)", example = "패션,겨울,스타일")
        private String tags;

        @Schema(description = "무드보드 이미지 URL", example = "https://example.com/moodboard.jpg")
        private String moodboardImageUrl;

        @Schema(description = "무드보드 설명", example = "따뜻한 겨울 분위기")
        private String moodboardDescription;

        @Schema(description = "작성자 아이디", example = "john_doe")
        private String username;

        @Schema(description = "공개 여부", example = "true")
        private Boolean isPublic;

        @Schema(description = "공유 토큰", example = "abc123xyz")
        private String shareToken;

        @Schema(description = "생성일시", example = "2024-12-23T10:30:00")
        private String createdAt;
    }

    @Schema(description = "매거진 목록 아이템")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListItem {
        @Schema(description = "매거진 ID", example = "1")
        private Long id;

        @Schema(description = "매거진 제목", example = "겨울철 패션 트렌드")
        private String title;

        @Schema(description = "매거진 부제", example = "따뜻함과 스타일을 동시에")
        private String subtitle;

        @Schema(description = "매거진 소개", example = "올 겨울 핫한 스타일링 가이드")
        private String introduction;

        @Schema(description = "커버 이미지 URL", example = "https://example.com/cover.jpg")
        private String coverImageUrl;

        @Schema(description = "작성자 아이디", example = "john_doe")
        private String username;

        @Schema(description = "좋아요 수", example = "42")
        private int likeCount;

        @Schema(description = "댓글 수", example = "5")
        private int commentCount;

        @Schema(description = "생성일시", example = "2024-12-23T10:30:00")
        private String createdAt;

        public static ListItem from(Magazine magazine) {
            return ListItem.builder()
                    .id(magazine.getId())
                    .title(magazine.getTitle())
                    .subtitle(magazine.getSubtitle())
                    .introduction(magazine.getIntroduction())
                    .coverImageUrl(magazine.getCoverImageUrl())
                    .username(magazine.getUser().getUsername())
                    .likeCount(0)
                    .commentCount(0)
                    .createdAt(magazine.getCreatedAt().toString())
                    .build();
        }
    }
}
