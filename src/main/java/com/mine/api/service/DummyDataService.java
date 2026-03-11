package com.mine.api.service;

import com.mine.api.domain.Role;
import com.mine.api.domain.User;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InterestService interestService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public void generateDummyData() {
        log.info("Starting dummy data generation...");

        // 1. 5명의 가계정 프로필과 관심사 정의
        Map<String, List<String>> dummyUsers = new HashMap<>();
        dummyUsers.put("dummy1", Arrays.asList("FASHION", "BEAUTY", "ART"));
        dummyUsers.put("dummy2", Arrays.asList("SPORTS", "HEALTH", "TRAVEL"));
        dummyUsers.put("dummy3", Arrays.asList("TECH", "SCIENCE", "GAME"));
        dummyUsers.put("dummy4", Arrays.asList("FOOD", "COOKING", "CAFE"));
        dummyUsers.put("dummy5", Arrays.asList("MUSIC", "MOVIE", "BOOK"));

        for (Map.Entry<String, List<String>> entry : dummyUsers.entrySet()) {
            String username = entry.getKey();
            List<String> interests = entry.getValue();

            // 이미 존재하는 계정인지 확인
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                // 2. 가계정 생성
                user = User.builder()
                        .username(username)
                        .email(username + "@test.com")
                        .password(passwordEncoder.encode("password123!"))
                        .nickname(username.toUpperCase() + " User")
                        .role(Role.USER)
                        .build();

                user = userRepository.save(user);
                log.info("Created dummy user: {}", username);

                // 3. 관심사 저장
                interestService.updateUserInterests(username, interests);

                // 4. 이벤트 발행을 통해 기존 비동기 자동 생성 로직 (3개 매거진) 태우기
                eventPublisher.publishEvent(new com.mine.api.event.UserSignupEvent(this, user, interests));
                log.info("Triggered magazine generation for dummy user: {}", username);
            } else {
                log.info("Dummy user already exists: {}", username);
            }
        }

        log.info("Dummy data generation triggered successfully.");
    }
}
