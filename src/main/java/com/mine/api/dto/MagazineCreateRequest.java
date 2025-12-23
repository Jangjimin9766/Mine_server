package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import lombok.Setter;

@Schema(description = "매거진 생성/저장 요청 (AI 서버에서 전달)")
@Getter
@Setter
@NoArgsConstructor
public class MagazineCreateRequest {

    @Schema(description = "매거진 제목", example = "겨울철 패션 트렌드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "매거진 부제", example = "따뜻함과 스타일을 동시에 잡는 법")
    private String subtitle;

    @Schema(description = "매거진 소개/도입부", example = "올 겨울, 패션과 따뜻함을 모두 잡는 스타일링 가이드를 소개합니다.")
    private String introduction;

    @Schema(description = "커버 이미지 URL", example = "https://example.com/cover.jpg")
    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    @Schema(description = "사용자 이메일 (내부 API에서 사용)", example = "user@example.com")
    @JsonProperty("user_email")
    private String userEmail;

    @Schema(description = "태그 목록", example = "[\"패션\", \"겨울\", \"스타일\"]")
    private List<String> tags;

    @Schema(description = "매거진 전용 무드보드 이미지")
    private MoodboardResponseDto moodboard;

    @Schema(description = "매거진 섹션 목록")
    private List<SectionDto> sections;

    @Schema(description = "매거진 섹션")
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SectionDto {
        @Schema(description = "섹션 제목", example = "코트 스타일링")
        private String heading;

        @Schema(description = "섹션 본문 내용", example = "올 겨울 가장 핫한 코트 스타일링을 소개합니다...")
        private String content;

        @Schema(description = "섹션 이미지 URL", example = "https://example.com/section1.jpg")
        @JsonProperty("image_url")
        private String imageUrl;

        @Schema(description = "레이아웃 힌트 (기존)", example = "image_left")
        @JsonProperty("layout_hint")
        private String layoutHint;

        @Schema(description = "레이아웃 타입", example = "hero", allowableValues = { "hero", "quote", "split_left",
                "split_right", "basic" })
        @JsonProperty("layout_type")
        private String layoutType;

        @Schema(description = "이미지 캡션", example = "2024 FW 트렌드 코트")
        private String caption;
    }
}
