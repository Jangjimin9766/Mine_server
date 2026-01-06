package com.mine.api.dto;

import lombok.Data;

import java.util.List;

public class InteractionDto {

    @Data
    public static class InteractRequest {
        private String message; // 사용자 메시지 (예: "첫 번째 섹션을 더 감성적으로 바꿔줘")
    }

    @Data
    public static class InteractResponse {
        private String message; // AI 응답 메시지
        private String actionType; // "regenerate_section", "add_section", "change_tone"
        private Long magazineId;
        private com.mine.api.domain.Magazine magazine; // [NEW] 업데이트된 매거진 전체 정보
    }

    @Data
    public static class InteractionHistory {
        private Long id;
        private String userMessage;
        private String aiResponse;
        private String actionType;
        private String createdAt;
    }
}
