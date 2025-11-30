package com.mine.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class MoodboardResponseDto {
    private String image_url; // Base64 String
    private String description;
}
