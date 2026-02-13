package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

public class AuthDto {

    @Schema(description = "회원가입 요청")
    @Data
    public static class SignupRequest {
        @Schema(description = "🔑 아이디 (영문/숫자)", example = "mine_lover", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @Schema(description = "📧 이메일 주소", example = "lover@mine.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @Schema(description = "🔒 비밀번호 (8자 이상)", example = "Mypassword1!", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;

        @Schema(description = "😀 닉네임 (화면에 표시, 선택사항 - 없으면 아이디 사용)", example = "감성충전")
        private String nickname;

        @Schema(description = "❤️ 관심사 목록 (최대 3개)", example = "[\"MINIMALISM\", \"MOVIE\", \"IT\"]")
        private java.util.List<String> interests;
    }

    @Schema(description = "로그인 요청")
    @Data
    public static class LoginRequest {
        @Schema(description = "🔑 아이디", example = "mine_lover", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;

        @Schema(description = "🔒 비밀번호", example = "Mypassword1!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
    }

    @Schema(description = "로그인 응답 (JWT 토큰)")
    @Data
    public static class TokenResponse {
        @Schema(description = "Access Token (API 요청 시 Authorization 헤더에 사용)", example = "eyJhbGciOiJIUzI1NiIsInR...")
        private String accessToken;

        @Schema(description = "토큰 타입", example = "Bearer")
        private String tokenType = "Bearer";

        @Schema(description = "Refresh Token (Access Token 갱신 시 사용)", example = "dGhpcyBpcyBhIHJl...")
        private String refreshToken;

        @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
        private Long expiresIn;

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public TokenResponse(String accessToken, String refreshToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    @Schema(description = "토큰 갱신 요청")
    @Data
    public static class RefreshRequest {
        @Schema(description = "Refresh Token", example = "dGhpcyBpcyBhIHJl...", requiredMode = Schema.RequiredMode.REQUIRED)
        @jakarta.validation.constraints.NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Schema(description = "토큰 갱신 응답")
    @Data
    public static class RefreshResponse {
        @Schema(description = "새로 발급된 Access Token", example = "eyJhbGciOiJIUzI1NiIsInR...")
        private String accessToken;

        @Schema(description = "만료 시간 (초)", example = "3600")
        private Long expiresIn;

        public RefreshResponse(String accessToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }
    }

    @Schema(description = "비밀번호 변경 요청")
    @Data
    public static class PasswordChangeRequest {
        @Schema(description = "🔒 현재 비밀번호", example = "Mypassword1!", requiredMode = Schema.RequiredMode.REQUIRED)
        @jakarta.validation.constraints.NotBlank(message = "Current password is required")
        private String currentPassword;

        @Schema(description = "✨ 새 비밀번호 (8자 이상)", example = "NewStrongPass1!", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }
}
