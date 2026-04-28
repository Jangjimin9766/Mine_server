package com.mine.api.config;

import com.mine.api.security.JwtAuthenticationFilter;
import com.mine.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final com.mine.api.repository.BlacklistedTokenRepository blacklistedTokenRepository;

    @Value("${mine.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173,https://minelover.com,https://www.minelover.com,https://api.minelover.com}")
    private List<String> allowedOriginPatterns;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: 단방향 해시 + salt 자동 적용 — 레인보우 테이블 공격 방어
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT 기반 무상태 인증 — CSRF 불필요, 세션 사용 안 함
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()                          // 로그인/회원가입은 인증 불필요
                        .requestMatchers("/api/internal/**").permitAll()                      // Python 서버 내부 API는 X-Internal-Key로 별도 인증
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/magazines/feed/search").permitAll()            // 둘러보기 검색은 비로그인 허용
                        .requestMatchers("/api/magazines/share/**").permitAll()               // 공유 링크 비로그인 접근
                        .requestMatchers("/api/interests").permitAll()                        // 회원가입 화면에서 관심사 목록 조회 필요
                        .requestMatchers("/api/magazines/public/**").permitAll()              // 공개 피드는 누구나 열람 가능
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/magazines/*/sections/*")
                        .permitAll()                                                          // 섹션 상세 비로그인 허용 (공개 계정만)
                        .requestMatchers("/images/**").permitAll()                            // 기본 프로필 이미지 등 정적 에셋
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, blacklistedTokenRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 모든 출처 허용 — 프론트엔드 도메인이 확정되면 구체적 origin으로 좁힐 것
        for (String allowedOriginPattern : allowedOriginPatterns) {
            configuration.addAllowedOriginPattern(allowedOriginPattern);
        }
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
