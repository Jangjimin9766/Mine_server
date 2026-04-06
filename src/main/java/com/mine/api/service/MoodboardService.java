package com.mine.api.service;

import com.mine.api.common.ErrorMessages;
import com.mine.api.domain.Moodboard;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.repository.MoodboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoodboardService {

    private final MoodboardRepository moodboardRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final com.mine.api.repository.MagazineRepository magazineRepository;
    private final S3Service s3Service;
    private final RunPodService runPodService;

    @Value("${python.api.moodboard-url}")
    private String moodboardApiUrl;

    public String createMoodboard(String username, MoodboardRequestDto requestDto) {
        // 0. Find User
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ErrorMessages.USER_NOT_FOUND));

        // 1. 요청 데이터 준비 (로컬용: action 포함, RunPod용: data에는 제외)
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("topic", requestDto.getTopic());
        data.put("user_mood", requestDto.getUser_mood());
        data.put("user_interests", requestDto.getUser_interests());
        data.put("magazine_tags", requestDto.getMagazine_tags());
        data.put("magazine_titles", requestDto.getMagazine_titles());

        java.util.Map<String, Object> responseBody;
        java.util.Map<String, Object> output;

        // 2. 로컬 vs RunPod 분기
        if (moodboardApiUrl.contains("localhost") || moodboardApiUrl.contains("127.0.0.1")) {
            // 로컬: sendSyncRequest 사용 (플랫 JSON, action 포함)
            data.put("action", "generate_moodboard");
            responseBody = runPodService.sendSyncRequest(moodboardApiUrl, data);
            output = responseBody; // 로컬은 output 래핑 없음
        } else {
            // RunPod: sendRequest 사용 (input 래핑 + 비동기 폴링)
            java.util.Map<String, Object> runPodInput = new java.util.HashMap<>();
            runPodInput.put("action", "generate_moodboard");
            runPodInput.put("data", data);
            responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);

            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> outputTemp = (java.util.Map<String, Object>) responseBody.get("output");
            output = outputTemp;
        }

        if (output == null) {
            throw new RuntimeException("Failed to generate moodboard: no output");
        }

        // Python 서버 success 필드 확인 및 로깅
        Boolean success = (Boolean) output.get("success");
        if (success != null && !success) {
            String errorType = (String) output.get("error_type");
            log.warn("Moodboard generation failed, using fallback image. error_type={}", errorType);
        }

        String base64Image = (String) output.get("image_url");
        String description = (String) output.get("description");

        if (base64Image == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        // base64 또는 URL 형태로 오면 S3에 업로드 — 클라이얰트에는 S3 URL만 노출해야 한다
        String s3Url = s3Service.uploadBase64ToS3(base64Image);

        // magazineId=null — 머거진 연결 없는 독립형 무드보드
        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId())
                .imageUrl(s3Url)
                .prompt(description)
                .magazineId(null) // standalone moodboard
                .build());

        return s3Url;
    }

    /**
     * 매거진 기반 무드보드 생성 (신규 API)
     * magazineId만으로 매거진 정보를 조회하여 무드보드를 생성합니다.
     */
    public String createMoodboardForMagazine(Long magazineId, String username) {
        // 1. User 조회
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ErrorMessages.USER_NOT_FOUND));

        // 2. Magazine 조회
        com.mine.api.domain.Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        // 3. 소유자 검증
        if (!magazine.getUser().getId().equals(user.getId())) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }

        // 4. Magazine에서 데이터 추출
        String topic = magazine.getTitle();
        java.util.List<String> magazineTags = new java.util.ArrayList<>();
        if (magazine.getTags() != null && !magazine.getTags().isEmpty()) {
            magazineTags = java.util.Arrays.asList(magazine.getTags().split(","));
        }

        // 5. Python 서버 요청 준비 (user_interests는 빈 리스트로 - 매거진 제목에 집중)
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("topic", topic);
        data.put("user_mood", ""); // 매거진 기반이므로 기본값
        data.put("user_interests", java.util.List.of()); // 빈 리스트 - 착오 방지
        data.put("magazine_tags", magazineTags);
        data.put("magazine_titles", java.util.List.of(topic));

        java.util.Map<String, Object> responseBody;
        java.util.Map<String, Object> output;

        // 7. 로컬 vs RunPod 분기
        if (moodboardApiUrl.contains("localhost") || moodboardApiUrl.contains("127.0.0.1")) {
            // 로컬: action 직접 포함
            data.put("action", "generate_moodboard");
            responseBody = runPodService.sendSyncRequest(moodboardApiUrl, data);
            output = responseBody;
        } else {
            java.util.Map<String, Object> runPodInput = new java.util.HashMap<>();
            runPodInput.put("action", "generate_moodboard");
            runPodInput.put("data", data);
            responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);

            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> outputTemp = (java.util.Map<String, Object>) responseBody.get("output");
            output = outputTemp;
        }

        if (output == null) {
            throw new RuntimeException("Failed to generate moodboard: no output");
        }

        // Python 서버 success 필드 확인 및 로깅
        Boolean success = (Boolean) output.get("success");
        if (success != null && !success) {
            String errorType = (String) output.get("error_type");
            log.warn("Moodboard generation failed for magazine, using fallback image. magazineId={}, error_type={}",
                    magazineId, errorType);
        }

        String base64Image = (String) output.get("image_url");
        String description = (String) output.get("description");

        if (base64Image == null) {
            throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MOODBOARD);
        }

        // 8. S3 업로드
        String s3Url = s3Service.uploadBase64ToS3(base64Image);

        // 9. DB 저장 (with magazineId)
        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId())
                .imageUrl(s3Url)
                .prompt(description)
                .magazineId(magazineId)
                .build());

        // 매거진 커버 = 무드보드 이미지 정책 — 매거진 생성 시마다 AI가 새 커버 자동 생성
        magazine.setMoodboardImageUrl(s3Url);
        magazine.setCoverImageUrl(s3Url);
        magazineRepository.save(magazine);

        return s3Url;
    }

    /**
     * 매거진의 무드보드 히스토리 조회
     */
    public java.util.List<Moodboard> getMoodboardHistory(Long magazineId, String username) {
        // 매거진 조회 및 소유자 확인
        com.mine.api.domain.Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ErrorMessages.USER_NOT_FOUND));

        if (!magazine.getUser().getId().equals(user.getId())) {
            throw new SecurityException("무드보드 히스토리 조회 권한이 없습니다");
        }

        return moodboardRepository.findByMagazineIdOrderByCreatedAtDesc(magazineId);
    }

    /**
     * 매거진 기반 무드보드 생성 (비동기 버전)
     * 이 메서드는 @Async 어노테이션을 사용하여 백그라운드에서 실행됩니다.
     */
    @org.springframework.scheduling.annotation.Async
    public void createMoodboardForMagazineAsync(Long magazineId, String username) {
        log.info("Async moodboard generation started for magazine: {}", magazineId);
        try {
            createMoodboardForMagazine(magazineId, username);
            log.info("Async moodboard generation completed for magazine: {}", magazineId);
        } catch (Exception e) {
            log.error("Async moodboard generation failed for magazine {}: {}", magazineId, e.getMessage());
        }
    }
}
