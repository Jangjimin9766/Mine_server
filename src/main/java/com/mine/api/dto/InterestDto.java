package com.mine.api.dto;

import com.mine.api.domain.Interest;
import lombok.Data;

import java.util.List;

public class InterestDto {

    @Data
    public static class InterestResponse {
        private String code;
        private String displayName;

        public InterestResponse(Interest interest) {
            this.code = interest.name();
            this.displayName = interest.getDisplayName();
        }
    }

    @Data
    public static class UpdateInterestsRequest {
        private List<String> interests; // Enum 코드 문자열 리스트 (예: ["LIFESTYLE", "BEAUTY"])
    }
}
