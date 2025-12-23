package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MagazineCreateRequest {

    private String title;

    // [NEW] 부제
    private String subtitle;

    private String introduction;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    @JsonProperty("user_email")
    private String userEmail;

    // [NEW] 태그 목록
    private List<String> tags;

    // [NEW] 매거진 내부 무드보드 (기존 MoodboardResponseDto와 동일 구조)
    private MoodboardResponseDto moodboard;

    private List<SectionDto> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SectionDto {
        private String heading;
        private String content;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("layout_hint")
        private String layoutHint;

        // [NEW] 레이아웃 타입: 'hero', 'quote', 'split_left', 'split_right', 'basic' 등
        @JsonProperty("layout_type")
        private String layoutType;

        // [NEW] 이미지 캡션 (Optional)
        private String caption;
    }
}
