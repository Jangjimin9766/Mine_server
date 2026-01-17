package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.repository.MoodboardRepository;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoodboardService {

    private final MoodboardRepository moodboardRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final com.mine.api.repository.MagazineRepository magazineRepository;
    private final com.mine.api.repository.UserInterestRepository userInterestRepository;
    private final S3Template s3Template;
    private final RunPodService runPodService;

    @Value("${python.api.moodboard-url}")
    private String moodboardApiUrl;

    @Value("${python.api.key}")
    private String pythonApiKey;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public String createMoodboard(String username, MoodboardRequestDto requestDto) {
        // 0. Find User
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. 요청 데이터 준비
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("action", "generate_moodboard");
        data.put("topic", requestDto.getTopic());
        data.put("user_mood", requestDto.getUser_mood());
        data.put("user_interests", requestDto.getUser_interests());
        data.put("magazine_tags", requestDto.getMagazine_tags());
        data.put("magazine_titles", requestDto.getMagazine_titles());

        java.util.Map<String, Object> responseBody;
        java.util.Map<String, Object> output;

        // 2. 로컬 vs RunPod 분기
        if (moodboardApiUrl.contains("localhost") || moodboardApiUrl.contains("127.0.0.1")) {
            // 로컬: sendSyncRequest 사용 (플랫 JSON)
            responseBody = runPodService.sendSyncRequest(moodboardApiUrl, data);
            output = responseBody; // 로컬은 output 래핑 없음
        } else {
            // RunPod: sendRequest 사용 (input 래핑 + 비동기 폴링)
            java.util.Map<String, Object> runPodInput = new java.util.HashMap<>();
            runPodInput.put("action", "generate_moodboard");
            runPodInput.put("data", data);
            responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);

            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException("Failed to generate moodboard image");
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> outputTemp = (java.util.Map<String, Object>) responseBody.get("output");
            output = outputTemp;
        }

        if (output == null) {
            throw new RuntimeException("Failed to generate moodboard: no output");
        }

        String base64Image = (String) output.get("image_url");
        String description = (String) output.get("description");

        if (base64Image == null) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        // 2, 3. Decode & Upload to S3
        String s3Url = uploadBase64ToS3(base64Image);

        // 4. Save to DB
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
    @Transactional
    public String createMoodboardForMagazine(Long magazineId, String username) {
        // 1. User 조회
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Magazine 조회
        com.mine.api.domain.Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found: " + magazineId));

        // 3. 소유자 검증
        if (!magazine.getUser().getId().equals(user.getId())) {
            throw new SecurityException("무드보드 생성 권한이 없습니다");
        }

        // 4. Magazine에서 데이터 추출
        String topic = magazine.getTitle();
        java.util.List<String> magazineTags = new java.util.ArrayList<>();
        if (magazine.getTags() != null && !magazine.getTags().isEmpty()) {
            magazineTags = java.util.Arrays.asList(magazine.getTags().split(","));
        }

        // 5. Python 서버 요청 준비 (user_interests는 빈 리스트로 - 매거진 제목에 집중)
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("action", "generate_moodboard");
        data.put("topic", topic);
        data.put("user_mood", ""); // 매거진 기반이므로 기본값
        data.put("user_interests", java.util.List.of()); // 빈 리스트 - 착오 방지
        data.put("magazine_tags", magazineTags);
        data.put("magazine_titles", java.util.List.of(topic));

        java.util.Map<String, Object> responseBody;
        java.util.Map<String, Object> output;

        // 7. 로컬 vs RunPod 분기
        if (moodboardApiUrl.contains("localhost") || moodboardApiUrl.contains("127.0.0.1")) {
            responseBody = runPodService.sendSyncRequest(moodboardApiUrl, data);
            output = responseBody;
        } else {
            java.util.Map<String, Object> runPodInput = new java.util.HashMap<>();
            runPodInput.put("action", "generate_moodboard");
            runPodInput.put("data", data);
            responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);

            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException("Failed to generate moodboard image");
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> outputTemp = (java.util.Map<String, Object>) responseBody.get("output");
            output = outputTemp;
        }

        if (output == null) {
            throw new RuntimeException("Failed to generate moodboard: no output");
        }

        String base64Image = (String) output.get("image_url");
        String description = (String) output.get("description");

        if (base64Image == null) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        // 8. S3 업로드
        String s3Url = uploadBase64ToS3(base64Image);

        // 9. DB 저장 (with magazineId)
        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId())
                .imageUrl(s3Url)
                .prompt(description)
                .magazineId(magazineId)
                .build());

        // 10. Magazine의 moodboardImageUrl 업데이트 (배경 자동 변경)
        magazine.setMoodboardImageUrl(s3Url);
        magazine.setMoodboardDescription(description);
        magazineRepository.save(magazine);

        return s3Url;
    }

    /**
     * Base64 이미지 S3 업로드 (공용)
     */
    public String uploadBase64ToS3(String base64Image) {
        if (base64Image == null)
            return null;

        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        String s3FileName = "moodboards/" + UUID.randomUUID() + ".png";
        s3Template.upload(bucketName, s3FileName, new ByteArrayInputStream(imageBytes));

        return "https://" + bucketName + ".s3.ap-southeast-2.amazonaws.com/" + s3FileName;
    }
}
