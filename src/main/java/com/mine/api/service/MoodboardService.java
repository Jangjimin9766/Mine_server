package com.mine.api.service;

import com.mine.api.common.ErrorMessages;
import com.mine.api.domain.Magazine;
import com.mine.api.domain.Moodboard;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.repository.MoodboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoodboardService {

    private static final int MAGAZINE_MOODBOARD_MAX_ATTEMPTS = 3;
    private static final long MAGAZINE_MOODBOARD_RETRY_DELAY_MS = 10_000L;

    private final MoodboardRepository moodboardRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final com.mine.api.repository.MagazineRepository magazineRepository;
    private final com.mine.api.repository.UserInterestRepository userInterestRepository;
    private final S3Service s3Service;
    private final RunPodService runPodService;
    private final ContentSafetyService contentSafetyService;

    @Value("${python.api.moodboard-url:${mine.internal.moodboard-url:}}")
    private String moodboardApiUrl;

    public String createMoodboard(String username, MoodboardRequestDto requestDto) {
        validateMoodboardApiUrl();
        validateMoodboardRequest(requestDto);

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ErrorMessages.USER_NOT_FOUND));

        Map<String, Object> data = new HashMap<>();
        data.put("topic", requestDto.getTopic());
        data.put("user_mood", requestDto.getUser_mood());
        data.put("user_interests", requestDto.getUser_interests());
        data.put("magazine_tags", requestDto.getMagazine_tags());
        data.put("magazine_titles", requestDto.getMagazine_titles());
        data.put("moodboard_rules", moodboardRules());

        Map<String, Object> output = callMoodboardApi(data);

        Boolean success = booleanValue(output.get("success"));
        if (success != null && !success) {
            log.warn("Moodboard generation returned success=false. error_type={}", stringValue(output.get("error_type")));
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        String imagePayload = firstNonBlank(stringValue(output.get("image_url")), stringValue(output.get("fallback_url")));
        String description = stringValue(output.get("description"));

        if (imagePayload == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        String s3Url = s3Service.uploadBase64ToS3(imagePayload);
        if (s3Url == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId())
                .imageUrl(s3Url)
                .prompt(description)
                .magazineId(null)
                .build());

        return s3Url;
    }

    /**
     * 매거진 기반 무드보드 생성 (동기 API).
     * 실패해도 Magazine 본문은 삭제/롤백하지 않고 moodboardStatus만 FAILED로 갱신한다.
     */
    public String createMoodboardForMagazine(Long magazineId, String username) {
        markMoodboardPending(magazineId);

        try {
            MoodboardGenerationResult result = generateMoodboardForMagazine(magazineId, username);
            completeMoodboard(magazineId, result);
            return result.imageUrl();
        } catch (RuntimeException e) {
            markMoodboardFailed(magazineId);
            throw e;
        } catch (Exception e) {
            markMoodboardFailed(magazineId);
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD, e);
        }
    }

    /**
     * 매거진 생성 응답을 막지 않는 비동기 무드보드 생성.
     */
    @Async
    public void createMoodboardForMagazineAsync(Long magazineId, String username) {
        log.info("Async moodboard generation started for magazine: {}", magazineId);
        markMoodboardPending(magazineId);

        for (int attempt = 1; attempt <= MAGAZINE_MOODBOARD_MAX_ATTEMPTS; attempt++) {
            try {
                MoodboardGenerationResult result = generateMoodboardForMagazine(magazineId, username);
                completeMoodboard(magazineId, result);
                log.info("Async moodboard generation completed for magazine: {}", magazineId);
                return;
            } catch (Exception e) {
                log.warn("Async moodboard generation attempt {}/{} failed for magazine {}: {}",
                        attempt, MAGAZINE_MOODBOARD_MAX_ATTEMPTS, magazineId, e.getMessage());

                if (attempt >= MAGAZINE_MOODBOARD_MAX_ATTEMPTS) {
                    markMoodboardFailed(magazineId);
                    log.error("Async moodboard generation failed for magazine {} after {} attempts",
                            magazineId, MAGAZINE_MOODBOARD_MAX_ATTEMPTS, e);
                    return;
                }

                sleepBeforeRetry(magazineId);
            }
        }
    }

    /**
     * 매거진의 무드보드 히스토리 조회
     */
    public List<Moodboard> getMoodboardHistory(Long magazineId, String username) {
        Magazine magazine = magazineRepository.findByIdAndUserUsername(magazineId, username)
                .orElseThrow(() -> magazineRepository.existsById(magazineId)
                        ? new SecurityException("무드보드 히스토리 조회 권한이 없습니다")
                        : new jakarta.persistence.EntityNotFoundException(ErrorMessages.MAGAZINE_NOT_FOUND));

        return moodboardRepository.findByMagazineIdOrderByCreatedAtDesc(magazine.getId());
    }

    private MoodboardGenerationResult generateMoodboardForMagazine(Long magazineId, String username) {
        validateMoodboardApiUrl();

        MagazineMoodboardContext context = buildMagazineMoodboardContext(magazineId, username);

        Map<String, Object> data = new HashMap<>();
        data.put("topic", context.topic());
        data.put("user_mood", context.userMood());
        data.put("user_interests", context.userInterests());
        data.put("magazine_tags", context.magazineTags());
        data.put("magazine_titles", context.magazineTitles());
        data.put("moodboard_rules", moodboardRules());

        Map<String, Object> output = callMoodboardApi(data);

        Boolean success = booleanValue(output.get("success"));
        if (success != null && !success) {
            throw new RuntimeException("Moodboard generation failed: " + stringValue(output.get("error_type")));
        }

        String imagePayload = firstNonBlank(stringValue(output.get("image_url")), stringValue(output.get("fallback_url")));
        String description = stringValue(output.get("description"));

        if (imagePayload == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        String s3Url = s3Service.uploadBase64ToS3(imagePayload);
        if (s3Url == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        return new MoodboardGenerationResult(context.userId(), s3Url, description);
    }

    private MagazineMoodboardContext buildMagazineMoodboardContext(Long magazineId, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ErrorMessages.USER_NOT_FOUND));

        Magazine magazine = magazineRepository.findByIdAndUserUsername(magazineId, username)
                .orElseThrow(() -> magazineRepository.existsById(magazineId)
                        ? new SecurityException(ErrorMessages.NOT_AUTHORIZED)
                        : new jakarta.persistence.EntityNotFoundException(ErrorMessages.MAGAZINE_NOT_FOUND));

        List<String> userInterests = userInterestRepository.findByUser(user).stream()
                .map(userInterest -> userInterest.getInterest().getCode())
                .filter(code -> code != null && !code.isBlank())
                .toList();

        List<String> magazineTags = parseTags(magazine.getTags());
        String topic = magazine.getTitle();
        List<String> magazineTitles = topic == null || topic.isBlank() ? List.of() : List.of(topic);

        return new MagazineMoodboardContext(
                user.getId(),
                topic,
                null,
                userInterests,
                magazineTags,
                magazineTitles);
    }

    private Map<String, Object> callMoodboardApi(Map<String, Object> data) {
        Map<String, Object> responseBody;

        if (isLocalMoodboardApi()) {
            data.put("action", "generate_moodboard");
            responseBody = runPodService.sendSyncRequest(moodboardApiUrl, data);
        } else {
            Map<String, Object> runPodInput = new HashMap<>();
            runPodInput.put("action", "generate_moodboard");
            runPodInput.put("data", data);
            responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);
        }

        return unwrapMoodboardOutput(responseBody);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapMoodboardOutput(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("Failed to generate moodboard: no response");
        }

        Object output = responseBody.containsKey("output") ? responseBody.get("output") : responseBody;
        if (!(output instanceof Map<?, ?> outputMap)) {
            throw new RuntimeException("Failed to generate moodboard: invalid output");
        }

        if (outputMap.containsKey("moodboard") && outputMap.get("moodboard") instanceof Map<?, ?> moodboardMap) {
            return (Map<String, Object>) moodboardMap;
        }

        return (Map<String, Object>) outputMap;
    }

    private void completeMoodboard(Long magazineId, MoodboardGenerationResult result) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(ErrorMessages.MAGAZINE_NOT_FOUND));

        magazine.completeMoodboard(result.imageUrl(), result.description());
        magazineRepository.save(magazine);

        saveMoodboardHistory(magazineId, result);
    }

    private void saveMoodboardHistory(Long magazineId, MoodboardGenerationResult result) {
        if (result.userId() == null) {
            log.warn("Skipping moodboard history save because userId is null. magazineId={}", magazineId);
            return;
        }

        try {
            moodboardRepository.save(Moodboard.builder()
                    .userId(result.userId())
                    .magazineId(magazineId)
                    .imageUrl(result.imageUrl())
                    .prompt(result.description())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save moodboard history. magazineId={}", magazineId, e);
        }
    }

    private void markMoodboardPending(Long magazineId) {
        magazineRepository.findById(magazineId).ifPresent(magazine -> {
            magazine.markMoodboardPending();
            magazineRepository.save(magazine);
        });
    }

    private void markMoodboardFailed(Long magazineId) {
        magazineRepository.findById(magazineId).ifPresent(magazine -> {
            magazine.failMoodboard();
            magazineRepository.save(magazine);
        });
    }

    private void sleepBeforeRetry(Long magazineId) {
        try {
            Thread.sleep(MAGAZINE_MOODBOARD_RETRY_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            markMoodboardFailed(magazineId);
            throw new RuntimeException("Moodboard retry interrupted", interrupted);
        }
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String tag : tags.split(",")) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void validateMoodboardApiUrl() {
        if (moodboardApiUrl == null || moodboardApiUrl.isBlank()) {
            throw new IllegalStateException("python.api.moodboard-url is missing (PYTHON_MOODBOARD_URL).");
        }
    }

    private void validateMoodboardRequest(MoodboardRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("무드보드 요청 본문이 필요합니다.");
        }
        contentSafetyService.validateText(requestDto.getTopic());
        contentSafetyService.validateText(requestDto.getUser_mood());
        contentSafetyService.validateTexts(requestDto.getUser_interests());
        contentSafetyService.validateTexts(requestDto.getMagazine_tags());
        contentSafetyService.validateTexts(requestDto.getMagazine_titles());
    }

    private List<String> moodboardRules() {
        return List.of(
                "topic, magazine_titles, magazine_tags와 직접 관련된 시각 요소만 사용한다.",
                "주제와 무관한 스포츠 장비, 골프공, 일반 소품 이미지는 사용하지 않는다.",
                "관련 이미지를 만들 수 없으면 무작위 대체 이미지를 반환하지 말고 실패로 응답한다.");
    }

    private boolean isLocalMoodboardApi() {
        return moodboardApiUrl.contains("localhost") || moodboardApiUrl.contains("127.0.0.1");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return null;
    }

    private record MagazineMoodboardContext(
            Long userId,
            String topic,
            String userMood,
            List<String> userInterests,
            List<String> magazineTags,
            List<String> magazineTitles) {
    }

    private record MoodboardGenerationResult(Long userId, String imageUrl, String description) {
    }
}
