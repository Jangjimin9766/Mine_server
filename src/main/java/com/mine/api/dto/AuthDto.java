package com.mine.api.dto;

import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        private String username; // 아이디
        private String email;
        private String password;
        private String nickname;
        private java.util.List<String> interests; // 관심사 (선택사항, 최대 3개)
    }

    @Data
    public static class LoginRequest {
        private String username; // 아이디로 로그인
        private String password;
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private String refreshToken; // ⭐ Phase 7: Refresh Token 추가
        private Long expiresIn; // ⭐ Phase 7: 만료 시간 추가

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public TokenResponse(String accessToken, String refreshToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    // ⭐ Phase 7: Refresh Token 요청
    @Data
    public static class RefreshRequest {
        @jakarta.validation.constraints.NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    // ⭐ Phase 7: Refresh Token 응답
    @Data
    public static class RefreshResponse {
        private String accessToken;
        private Long expiresIn;

        public RefreshResponse(String accessToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }
    }

    // ⭐ Phase 7: 비밀번호 변경 요청
    @Data
    public static class PasswordChangeRequest {
        @jakarta.validation.constraints.NotBlank(message = "Current password is required")
        private String currentPassword;

        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }
}
