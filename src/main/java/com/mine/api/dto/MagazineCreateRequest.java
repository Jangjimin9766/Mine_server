package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MagazineCreateRequest {

    private String title;
    private String introduction;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    private List<SectionDto> sections;

    @Getter
    @NoArgsConstructor
    public static class SectionDto {
        private String heading;
        private String content;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("layout_hint")
        private String layoutHint;
    }
}
