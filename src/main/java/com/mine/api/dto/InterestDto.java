package com.mine.api.dto;

import com.mine.api.domain.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class InterestDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestResponse {
        private Long id;
        private String code;
        private String name;
        private String category;

        public static InterestResponse from(Interest interest) {
            return InterestResponse.builder()
                    .id(interest.getId())
                    .code(interest.getCode())
                    .name(interest.getName())
                    .category(interest.getCategory())
                    .build();
        }
    }

    @Data
    public static class UpdateInterestsRequest {
        private List<String> interests; // 코드 문자열 리스트 (예: ["FASHION", "BEAUTY"])
    }
}
