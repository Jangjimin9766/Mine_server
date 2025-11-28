package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MagazineGenerationRequest {
    private String topic;

    @JsonProperty("user_mood")
    private String userMood;
}
