package com.mine.api.controller;

import com.mine.api.dto.UserDto;
import com.mine.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. 사용자 (User) 👤", description = "사용자 프로필 조회/수정 및 팔로우 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ⭐ Phase 5: 내 프로필 조회
    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserDto.ProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(userDetails.getUsername()));
    }

    // ⭐ Phase 5: 프로필 수정
    @Operation(summary = "프로필 수정", description = "로그인한 사용자의 프로필(닉네임, 소개, 이미지)을 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<UserDto.ProfileResponse> updateMyProfile(
            @RequestBody @jakarta.validation.Valid UserDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    // ⭐ Phase 5: 다른 사용자 프로필 조회
    @Operation(summary = "사용자 프로필 조회", description = "다른 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto.ProfileResponse> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(userService.getUserProfile(userId, currentUsername));
    }

    // ⭐ Phase 6: 회원 탈퇴
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 진행합니다. (Soft Delete)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String token) {

        userService.withdrawUser(userDetails.getUsername());

        if (token != null && token.startsWith("Bearer ")) {
            userService.logout(token.substring(7));
        }

        return ResponseEntity.noContent().build();
    }

    // ⭐ 계정 공개/비공개 설정
    @Operation(summary = "🔒 계정 공개 설정", description = "계정을 공개 또는 비공개로 설정합니다. 비공개 계정의 매거진은 다른 사용자에게 보이지 않습니다.")
    @PatchMapping("/me/visibility")
    public ResponseEntity<?> setAccountVisibility(
            @RequestBody java.util.Map<String, Boolean> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Boolean isPublic = request.get("isPublic");
        if (isPublic == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "isPublic 값이 필요합니다"));
        }
        userService.setAccountVisibility(userDetails.getUsername(), isPublic);
        return ResponseEntity.ok(
                java.util.Map.of("isPublic", isPublic, "message", isPublic ? "계정이 공개로 설정되었습니다" : "계정이 비공개로 설정되었습니다"));
    }

    // ⭐ Phase 4: 팔로우
    @Operation(summary = "팔로우", description = "특정 사용자를 팔로우합니다.")
    @PostMapping("/{userId}/follow")
    public ResponseEntity<UserDto.FollowResponse> followUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(userService.followUser(userId, userDetails.getUsername()));
    }

    // ⭐ Phase 4: 언팔로우
    @Operation(summary = "언팔로우", description = "특정 사용자를 언팔로우합니다.")
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<UserDto.FollowResponse> unfollowUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(userService.unfollowUser(userId, userDetails.getUsername()));
    }

    // ⭐ Phase 4: 팔로워 목록
    @Operation(summary = "팔로워 목록", description = "특정 사용자를 팔로우하는 사람들의 목록을 조회합니다.")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserDto.ProfileResponse>> getFollowers(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(userService.getFollowers(userId, currentUsername, pageable));
    }

    // ⭐ Phase 4: 팔로잉 목록
    @Operation(summary = "팔로잉 목록", description = "특정 사용자가 팔로우하는 사람들의 목록을 조회합니다.")
    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserDto.ProfileResponse>> getFollowing(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String currentUsername = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(userService.getFollowing(userId, currentUsername, pageable));
    }
}
