package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mine.api.domain.Magazine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

public class MagazineDto {

    @Schema(name = "MagazineUpdateRequest", description = "매거진 수정 요청")
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

        @JsonIgnore
        public boolean isValid() {
            return (title != null && !title.trim().isEmpty()) ||
                    (introduction != null && !introduction.trim().isEmpty());
        }
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
        @JsonProperty("magazineId")
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

        @Schema(description = "무드보드 설명", example = "따뜻한 겨울 분위기의 감성적인 스타일")
        private String moodboardDescription;

        @Schema(description = "좋아요 수", example = "42")
        private int likeCount;

        @Schema(description = "생성일시", example = "2024-12-23T10:30:00")
        private String createdAt;
    }

    @Schema(description = "매거진 목록 아이템")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListItem {
        @Schema(description = "매거진 ID", example = "1")
        @JsonProperty("magazineId")
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
                    .likeCount(magazine.getLikes().size())
                    .commentCount(0)
                    .createdAt(magazine.getCreatedAt().toString())
                    .build();
        }
    }

    /**
     * 매거진 상세 조회 응답 DTO
     * Entity 직접 반환 대신 사용하여 필요한 필드만 노출
     */
    @Schema(name = "MagazineDetailResponse", description = "매거진 상세 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class DetailResponse {
        @Schema(description = "매거진 ID", example = "1")
        @JsonProperty("magazineId")
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

        @Schema(description = "무드보드 설명", example = "따뜻한 겨울 분위기의 감성적인 스타일")
        private String moodboardDescription;

        @Schema(description = "좋아요 수", example = "42")
        @JsonProperty("likeCount")
        private int likeCount;

        @Schema(description = "생성일시", example = "2024-12-23T10:30:00")
        private String createdAt;

        @Schema(description = "작성자 정보")
        private SimpleUser user;

        @Schema(description = "섹션 목록")
        private List<SectionItem> sections;

        /**
         * 작성자 정보
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class SimpleUser {
            @Schema(description = "사용자 ID", example = "1")
            private Long id;

            @Schema(description = "사용자 아이디", example = "john_doe")
            private String username;

            @Schema(description = "사용자 닉네임", example = "존도")
            private String nickname;

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            private String profileImageUrl;

            @Schema(description = "공개 계정 여부", example = "true")
            private Boolean isPublic;

            @Schema(description = "팔로워 수", example = "42")
            private Integer followerCount;

            @Schema(description = "매거진 수", example = "5")
            private Integer magazineCount;
        }

        /**
         * 섹션 정보 (필수 필드만 포함)
         */
        @Getter
        @Builder
        @AllArgsConstructor
        public static class SectionItem {
            @Schema(description = "섹션 ID", example = "101")
            @JsonProperty("sectionId")
            private Long id;

            @Schema(description = "소제목", example = "서울의 숨겨진 카페")
            private String heading;

            @Schema(description = "섹션 썸네일 URL", example = "https://example.com/thumbnail.jpg")
            private String thumbnailUrl;

            @Schema(description = "문단 배열")
            private List<ParagraphDto.Response> paragraphs;

            @Schema(description = "레이아웃 타입", example = "card")
            private String layoutType;

            @Schema(description = "표시 순서", example = "1")
            private Integer displayOrder;
        }

        /**
         * Magazine Entity를 DetailResponse로 변환
         */
        public static DetailResponse from(Magazine magazine) {
            SimpleUser simpleUser = SimpleUser.builder()
                    .id(magazine.getUser().getId())
                    .username(magazine.getUser().getUsername())
                    .nickname(magazine.getUser().getNickname())
                    .profileImageUrl(magazine.getUser().getProfileImageUrl())
                    .isPublic(magazine.getUser().getIsPublic())
                    .followerCount(magazine.getUser().getFollowerCount())
                    .magazineCount(magazine.getUser().getMagazineCount())
                    .build();

            List<SectionItem> sectionItems = magazine.getSections().stream()
                    .map(section -> SectionItem.builder()
                            .id(section.getId())
                            .heading(section.getHeading())
                            .thumbnailUrl(section.getThumbnailUrl())
                            .paragraphs(section.getParagraphs().stream()
                                    .map(p -> ParagraphDto.Response.builder()
                                            .subtitle(p.getSubtitle())
                                            .text(p.getText())
                                            .imageUrl(p.getImageUrl())
                                            .build())
                                    .toList())
                            .layoutType(section.getLayoutType())
                            .displayOrder(section.getDisplayOrder())
                            .build())
                    .toList();

            return DetailResponse.builder()
                    .id(magazine.getId())
                    .title(magazine.getTitle())
                    .subtitle(magazine.getSubtitle())
                    .introduction(magazine.getIntroduction())
                    .coverImageUrl(magazine.getCoverImageUrl())
                    .tags(magazine.getTags())
                    .moodboardImageUrl(magazine.getMoodboardImageUrl())
                    .moodboardDescription(magazine.getMoodboardDescription())
                    .likeCount(magazine.getLikes().size()) // 좋아요 수 계산
                    .createdAt(magazine.getCreatedAt() != null ? magazine.getCreatedAt().toString() : null)
                    .user(simpleUser)
                    .sections(sectionItems)
                    .build();
        }
    }
}
