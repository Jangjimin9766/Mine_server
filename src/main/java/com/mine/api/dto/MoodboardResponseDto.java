package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Schema(description = "무드보드 응답")
@Getter
@Setter
@NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class MoodboardResponseDto {
    @Schema(description = "무드보드 이미지 URL", example = "https://example.com/moodboard.jpg")
    @com.fasterxml.jackson.annotation.JsonProperty("image_url")
    private String image_url;

    @Schema(description = "무드보드 설명", example = "따뜻하고 아늑한 겨울 분위기의 무드보드")
    @com.fasterxml.jackson.annotation.JsonProperty("description")
    private String description;
}
