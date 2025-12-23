package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "무드보드 생성 요청")
@Getter
@Builder
public class MoodboardRequestDto {
    @Schema(description = "무드보드 주제", example = "따뜻한 겨울 분위기")
    private String topic;

    @Schema(description = "사용자 기분/분위기", example = "아늑하고 편안한")
    private String user_mood;

    @Schema(description = "사용자 관심사 목록", example = "[\"패션\", \"인테리어\"]")
    private List<String> user_interests;

    @Schema(description = "매거진 태그 목록", example = "[\"겨울\", \"따뜻함\"]")
    private List<String> magazine_tags;

    @Schema(description = "매거진 제목 목록", example = "[\"겨울 패션 트렌드\"]")
    private List<String> magazine_titles;
}
