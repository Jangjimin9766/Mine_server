package com.mine.api.service;

import com.mine.api.domain.Role;
import com.mine.api.domain.User;
import com.mine.api.dto.AuthDto;
import com.mine.api.repository.UserRepository;
import com.mine.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final InterestService interestService;
    private final com.mine.api.repository.RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Long signup(AuthDto.SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        String nickname = (request.getNickname() != null && !request.getNickname().isBlank())
                ? request.getNickname()
                : request.getUsername();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // 관심사 저장 (선택사항)
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            interestService.updateUserInterests(savedUser.getUsername(), request.getInterests());
        }

        return savedUser.getId();
    }

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Access Token 생성
        String accessToken = jwtTokenProvider.createToken(user.getUsername(), user.getRole().name());

        // ⭐ Phase 7: Refresh Token 생성 및 저장
        com.mine.api.domain.RefreshToken refreshToken = com.mine.api.domain.RefreshToken.builder()
                .username(user.getUsername())
                .build();

        // 기존 Refresh Token 삭제 후 새로 저장
        // 기존 Refresh Token 삭제 후 새로 저장
        java.util.List<com.mine.api.domain.RefreshToken> oldTokens = refreshTokenRepository
                .findByUsername(user.getUsername());
        refreshTokenRepository.deleteAll(oldTokens);
        refreshTokenRepository.save(refreshToken);

        return new AuthDto.TokenResponse(accessToken, refreshToken.getToken(), 3600L);
    }

    // ⭐ Phase 7: Refresh Token으로 Access Token 갱신
    @Transactional
    public AuthDto.RefreshResponse refresh(String refreshTokenValue) {
        // 1. Refresh Token 조회
        com.mine.api.domain.RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // 2. 만료 확인 (Redis는 TTL로 자동 삭제되지만, 혹시 남아있을 경우를 대비)
        // RedisHash의 timeToLive가 동작하므로 별도 로직 불필요, 다만 조회된 시점에서 null 체크는 findByToken에서
        // 처리됨

        // 3. 새로운 Access Token 생성
        String username = refreshToken.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = jwtTokenProvider.createToken(user.getUsername(), user.getRole().name());

        // 4. Refresh Token Rotation: 기존 토큰 삭제 후 새로 생성
        refreshTokenRepository.delete(refreshToken);
        com.mine.api.domain.RefreshToken newRefreshToken = com.mine.api.domain.RefreshToken.builder()
                .username(username)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return new AuthDto.RefreshResponse(newAccessToken, 3600L);
    }

    // ⭐ Phase 7: 로그아웃 (Refresh Token 삭제)
    @Transactional
    public void logout(String username) {
        // User 존재 여부 확인 (선택사항)
        if (!userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("User not found");
        }

        java.util.List<com.mine.api.domain.RefreshToken> tokens = refreshTokenRepository.findByUsername(username);
        refreshTokenRepository.deleteAll(tokens);
    }

    // ⭐ Phase 7: 비밀번호 변경
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 2. 새 비밀번호로 변경
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 3. 보안을 위해 모든 Refresh Token 삭제 (재로그인 필요)
        // 3. 보안을 위해 모든 Refresh Token 삭제 (재로그인 필요)
        java.util.List<com.mine.api.domain.RefreshToken> tokens = refreshTokenRepository.findByUsername(username);
        refreshTokenRepository.deleteAll(tokens);
    }
}
