package com.mine.api.service;

import com.mine.api.domain.Follow;
import com.mine.api.domain.User;
import com.mine.api.dto.UserDto;
import com.mine.api.repository.FollowRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

        private final UserRepository userRepository;
        private final FollowRepository followRepository;
        private final com.mine.api.repository.MagazineRepository magazineRepository;
        private final com.mine.api.repository.BlacklistedTokenRepository blacklistedTokenRepository;
        private final com.mine.api.security.JwtTokenProvider jwtTokenProvider;
        private final com.mine.api.repository.UserInterestRepository userInterestRepository;

        /**
         * 팔로우 하기
         */
        @Transactional
        public UserDto.FollowResponse followUser(Long targetUserId, String currentUsername) {
                User follower = userRepository.findByUsername(currentUsername)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                User following = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

                if (follower.getId().equals(following.getId())) {
                        throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다");
                }

                if (followRepository.existsByFollowerAndFollowing(follower, following)) {
                        throw new IllegalArgumentException("이미 팔로우 중입니다");
                }

                Follow follow = Follow.builder()
                                .follower(follower)
                                .following(following)
                                .build();

                followRepository.save(follow);

                int followerCount = (int) followRepository.countByFollowing(following);
                return new UserDto.FollowResponse(true, followerCount);
        }

        /**
         * 언팔로우 하기
         */
        @Transactional
        public UserDto.FollowResponse unfollowUser(Long targetUserId, String currentUsername) {
                User follower = userRepository.findByUsername(currentUsername)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                User following = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

                Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                                .orElseThrow(() -> new IllegalArgumentException("팔로우 중이 아닙니다"));

                followRepository.delete(follow);

                int followerCount = (int) followRepository.countByFollowing(following);
                return new UserDto.FollowResponse(false, followerCount);
        }

        /**
         * 팔로워 목록 조회 (특정 유저를 팔로우하는 사람들)
         */
        public Page<UserDto.ProfileResponse> getFollowers(Long userId, String currentUsername, Pageable pageable) {
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                // 로그인한 사용자 정보 (팔로우 여부 확인용)
                User currentUser = currentUsername != null ? userRepository.findByUsername(currentUsername).orElse(null)
                                : null;

                return followRepository.findByFollowing(targetUser, pageable)
                                .map(follow -> convertToProfileResponse(follow.getFollower(), currentUser));
        }

        /**
         * 팔로잉 목록 조회 (특정 유저가 팔로우하는 사람들)
         */
        public Page<UserDto.ProfileResponse> getFollowing(Long userId, String currentUsername, Pageable pageable) {
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                User currentUser = currentUsername != null ? userRepository.findByUsername(currentUsername).orElse(null)
                                : null;

                return followRepository.findByFollower(targetUser, pageable)
                                .map(follow -> convertToProfileResponse(follow.getFollowing(), currentUser));
        }

        /**
         * 내 프로필 조회
         */
        public UserDto.ProfileResponse getMyProfile(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                return convertToProfileResponseWithPassword(user);
        }

        /**
         * 다른 사용자 프로필 조회
         */
        public UserDto.ProfileResponse getUserProfile(Long userId, String currentUsername) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                User currentUser = currentUsername != null ? userRepository.findByUsername(currentUsername).orElse(null)
                                : null;

                return convertToProfileResponse(user, currentUser);
        }

        /**
         * 프로필 수정
         */
        @Transactional
        public UserDto.ProfileResponse updateProfile(String username, UserDto.UpdateRequest request) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                // username 변경 시 중복 체크
                if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                                throw new IllegalArgumentException("이미 사용 중인 아이디입니다");
                        }
                }

                user.updateProfile(request.getNickname(), request.getProfileImageUrl());

                return convertToProfileResponseWithPassword(user);
        }

        /**
         * 회원 탈퇴 (Soft Delete)
         */
        @Transactional
        public void withdrawUser(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

                user.softDelete();
        }

        /**
         * 로그아웃 / 회원 탈퇴 시 토큰 블랙리스트 추가
         */
        @Transactional
        public void logout(String token) {
                long expiration = jwtTokenProvider.getExpiration(token);
                if (expiration > 0) {
                        com.mine.api.domain.BlacklistedToken blacklistedToken = new com.mine.api.domain.BlacklistedToken(
                                        token,
                                        java.time.LocalDateTime.now().plusNanos(expiration * 1000000));
                        blacklistedTokenRepository.save(blacklistedToken);
                }
        }

        /**
         * 계정 공개/비공개 설정
         */
        @Transactional
        public void setAccountVisibility(String username, boolean isPublic) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                user.setPublic(isPublic);
        }

        private UserDto.ProfileResponse convertToProfileResponse(User user, User currentUser) {
                boolean isFollowing = false;
                if (currentUser != null && !currentUser.getId().equals(user.getId())) {
                        isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, user);
                }

                // 관심사 목록 조회
                java.util.List<String> interests = userInterestRepository.findByUser(user)
                                .stream()
                                .map(ui -> ui.getInterest().getName())
                                .toList();

                return UserDto.ProfileResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .nickname(user.getNickname())
                                .email(user.getEmail())
                                .profileImageUrl(user.getProfileImageUrl())
                                .followerCount(user.getFollowerCount())
                                .followingCount(user.getFollowingCount())
                                .magazineCount(user.getMagazineCount())
                                .isPublic(user.getIsPublic())
                                .interests(interests)
                                .isFollowing(isFollowing)
                                .build();
        }

        /**
         * 내 프로필 조회 시 비밀번호 포함 (본인만)
         */
        private UserDto.ProfileResponse convertToProfileResponseWithPassword(User user) {
                // 관심사 목록 조회
                java.util.List<String> interests = userInterestRepository.findByUser(user)
                                .stream()
                                .map(ui -> ui.getInterest().getName())
                                .toList();

                return UserDto.ProfileResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .password(user.getPassword()) // 비밀번호 반환
                                .nickname(user.getNickname())
                                .email(user.getEmail())
                                .profileImageUrl(user.getProfileImageUrl())
                                .followerCount(user.getFollowerCount())
                                .followingCount(user.getFollowingCount())
                                .magazineCount(user.getMagazineCount())
                                .isPublic(user.getIsPublic())
                                .interests(interests)
                                .isFollowing(false) // 본인이므로 false
                                .build();
        }
}
