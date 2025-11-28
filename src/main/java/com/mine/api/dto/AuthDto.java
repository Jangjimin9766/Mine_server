package com.mine.api.dto;

import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        private String username; // 아이디
        private String email;
        private String password;
        private String nickname;
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

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
