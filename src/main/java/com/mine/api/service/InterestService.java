package com.mine.api.service;

import com.mine.api.domain.Interest;
import com.mine.api.domain.User;
import com.mine.api.domain.UserInterest;
import com.mine.api.dto.InterestDto;
import com.mine.api.repository.UserInterestRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;

    public List<InterestDto.InterestResponse> getAllInterests() {
        return Arrays.stream(Interest.values())
                .map(InterestDto.InterestResponse::new)
                .collect(Collectors.toList());
    }

    public List<InterestDto.InterestResponse> getUserInterests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userInterestRepository.findByUser(user).stream()
                .map(ui -> new InterestDto.InterestResponse(ui.getInterest()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserInterests(String username, List<String> interestCodes) {
        if (interestCodes.size() > 3) {
            throw new IllegalArgumentException("You can select up to 3 interests only");
        }

        // String을 Interest Enum으로 변환
        List<Interest> interests = interestCodes.stream()
                .map(code -> {
                    try {
                        return Interest.valueOf(code.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid interest code: " + code);
                    }
                })
                .collect(Collectors.toList());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 기존 관심사 삭제
        userInterestRepository.deleteByUser(user);

        // 새 관심사 저장
        for (Interest interest : interests) {
            UserInterest userInterest = UserInterest.builder()
                    .user(user)
                    .interest(interest)
                    .build();
            userInterestRepository.save(userInterest);
        }
    }
}
