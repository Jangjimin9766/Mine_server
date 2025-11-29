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
    public static class ProfileResponse {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String bio;
        private String profileImageUrl;
        private Integer followerCount;
        private Integer followingCount;
        private Integer magazineCount;

        // 팔로우 여부 (로그인한 사용자가 이 사람을 팔로우하는지)
        private Boolean isFollowing;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
        private String nickname;

        @Size(max = 200, message = "소개는 200자 이내여야 합니다")
        private String bio;

        @Email(message = "이메일 형식이 올바르지 않습니다")
        private String email;

        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class FollowResponse {
        private boolean followed;
        private int followerCount;
    }
}
