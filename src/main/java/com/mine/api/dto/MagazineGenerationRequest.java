package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "AI 매거진 생성 요청")
@Data
@NoArgsConstructor
public class MagazineGenerationRequest {

    @Schema(description = "매거진 주제 (예: 겨울 패션, 여행 추천)", example = "겨울철 따뜻한 패션 트렌드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String topic;

    @Schema(description = "사용자의 현재 기분/분위기 (AI가 톤에 반영)", example = "따뜻하고 아늑한 느낌")
    @JsonProperty("user_mood")
    private String userMood;
}
