package com.mine.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MoodboardRequestDto {
    private String topic;
    private String user_mood;
    private List<String> user_interests;
    private List<String> magazine_tags;
    private List<String> magazine_titles;
}
