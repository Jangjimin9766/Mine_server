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

    @Transactional
    public Long signup(AuthDto.SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // 관심사 저장 (선택사항)
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            interestService.updateUserInterests(savedUser.getUsername(), request.getInterests());
        }

        return savedUser.getId();
    }

    @Transactional(readOnly = true)
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole().name());
        return new AuthDto.TokenResponse(token);
    }
}
