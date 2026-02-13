package com.mine.api.controller;

import com.mine.api.dto.AuthDto;
import com.mine.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "0. 인증 (Auth) 🔐", description = "회원가입, 로그인 및 토큰 관리 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "✨ 회원가입 (Sign Up)", description = "서비스 이용을 위해 새로운 계정을 만듭니다.")
    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody @Valid AuthDto.SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @Operation(summary = "🔑 로그인 (Login)", description = "아이디와 비밀번호로 로그인하여 Access Token을 받습니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ⭐ Phase 7: Refresh Token으로 Access Token 갱신
    @Operation(summary = "🔄 토큰 갱신 (Refresh Token)", description = "만료된 Access Token을 Refresh Token으로 갱신합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.RefreshResponse> refresh(@RequestBody AuthDto.RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    // ⭐ Phase 7: 로그아웃
    @Operation(summary = "👋 로그아웃 (Logout)", description = "안전하게 로그아웃하고 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // ⭐ Phase 7: 비밀번호 변경
    @Operation(summary = "🔐 비밀번호 변경 (Change Password)", description = "현재 비밀번호를 확인하고 새 비밀번호로 바꿉니다.")
    @org.springframework.web.bind.annotation.PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @RequestBody AuthDto.PasswordChangeRequest request) {
        authService.changePassword(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
