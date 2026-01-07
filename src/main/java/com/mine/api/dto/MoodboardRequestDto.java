package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "무드보드 생성 요청")
@Getter
@Builder
public class MoodboardRequestDto {
    @Schema(description = "무드보드 주제", example = "사이버펑크 네온 시티")
    private String topic;

    @Schema(description = "사용자 기분/분위기 (AI 반영)", example = "활기차고 미래적인 느낌")
    private String user_mood;

    @Schema(description = "사용자 관심사 목록 (코드)", example = "[\"CYBERPUNK\", \"GAME\", \"IT\"]")
    private List<String> user_interests;

    @Schema(description = "매거진 태그 목록", example = "[\"네온\", \"밤거리\", \"미래\"]")
    private List<String> magazine_tags;

    @Schema(description = "매거진 제목 목록", example = "[\"미래 도시의 밤\", \"사이버펑크 스타일 가이드\"]")
    private List<String> magazine_titles;
}
