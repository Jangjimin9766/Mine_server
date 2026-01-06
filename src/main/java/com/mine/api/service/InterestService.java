package com.mine.api.service;

import com.mine.api.domain.Interest;
import com.mine.api.domain.User;
import com.mine.api.domain.UserInterest;
import com.mine.api.dto.InterestDto;
import com.mine.api.repository.InterestRepository;
import com.mine.api.repository.UserInterestRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;

    /**
     * 모든 관심분야 목록 조회
     */
    public List<InterestDto.InterestResponse> getAllInterests() {
        return interestRepository.findAll().stream()
                .map(InterestDto.InterestResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 관심분야 조회
     */
    public List<InterestDto.InterestResponse> getInterestsByCategory(String category) {
        return interestRepository.findByCategory(category).stream()
                .map(InterestDto.InterestResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 관심분야 조회
     */
    public List<InterestDto.InterestResponse> getUserInterests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userInterestRepository.findByUser(user).stream()
                .map(ui -> InterestDto.InterestResponse.from(ui.getInterest()))
                .collect(Collectors.toList());
    }

    /**
     * 사용자 관심분야 업데이트 (최대 3개)
     */
    @Transactional
    public void updateUserInterests(String username, List<String> interestCodes) {
        if (interestCodes.size() > 3) {
            throw new IllegalArgumentException("You can select up to 3 interests only");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 코드로 Interest 엔티티 조회
        List<Interest> interests = interestCodes.stream()
                .map(code -> interestRepository.findByCode(code.toUpperCase())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid interest code: " + code)))
                .collect(Collectors.toList());

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
