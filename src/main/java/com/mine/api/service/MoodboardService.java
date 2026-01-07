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

        // 1. RunPod Serverless 요청 (input wrapper 형식)
        java.util.Map<String, Object> inputData = new java.util.HashMap<>();
        inputData.put("action", "generate_moodboard");
        inputData.put("topic", requestDto.getTopic());
        inputData.put("user_mood", requestDto.getUser_mood());
        inputData.put("user_interests", requestDto.getUser_interests());
        inputData.put("magazine_tags", requestDto.getMagazine_tags());
        inputData.put("magazine_titles", requestDto.getMagazine_titles());

        // 2. RunPod Serverless로 요청 (Async Polling)
        java.util.Map<String, Object> runPodInput = new java.util.HashMap<>();
        runPodInput.put("action", "generate_moodboard");
        runPodInput.put("data", inputData); // Wrap the previous inputData as 'data'

        java.util.Map<String, Object> responseBody = runPodService.sendRequest(moodboardApiUrl, runPodInput);

        if (responseBody == null || !responseBody.containsKey("output")) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> output = (java.util.Map<String, Object>) responseBody.get("output");
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
                .build());

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
