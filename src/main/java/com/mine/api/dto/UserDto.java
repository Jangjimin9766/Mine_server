package com.mine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @io.swagger.v3.oas.annotations.media.Schema(name = "UserProfileResponse")
    public static class ProfileResponse {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String profileImageUrl;
        private Integer followerCount;
        private Integer followingCount;
        private Integer magazineCount;
        private Boolean isPublic; // 계정 공개 여부
        private java.util.List<String> interests; // 관심사 목록

        // 팔로우 여부 (로그인한 사용자가 이 사람을 팔로우하는지)
        private Boolean isFollowing;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @io.swagger.v3.oas.annotations.media.Schema(name = "UserUpdateRequest", description = "프로필 수정 요청")
    public static class UpdateRequest {
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
        @io.swagger.v3.oas.annotations.media.Schema(description = "닉네임", example = "감성충전")
        private String nickname;

        @Email(message = "이메일 형식이 올바르지 않습니다")
        @io.swagger.v3.oas.annotations.media.Schema(description = "이메일", example = "user@example.com")
        private String email;

        @io.swagger.v3.oas.annotations.media.Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class FollowResponse {
        private boolean followed;
        private int followerCount;
    }
}
