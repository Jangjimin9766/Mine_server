package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.repository.MoodboardRepository;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoodboardService {

    private final MoodboardRepository moodboardRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final S3Template s3Template;
    private final WebClient.Builder webClientBuilder;

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

        java.util.Map<String, Object> runpodRequest = new java.util.HashMap<>();
        runpodRequest.put("input", inputData);

        // Increase buffer size to handle large Base64 images (default is 256KB)
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> runpodResponse = webClientBuilder.exchangeStrategies(strategies).build()
                .post()
                .uri(moodboardApiUrl)
                .header("Authorization", "Bearer " + pythonApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(runpodRequest)
                .retrieve()
                .bodyToMono((Class<java.util.Map<String, Object>>) (Class<?>) java.util.Map.class)
                .block(java.time.Duration.ofSeconds(180)); // RunPod 콜드스타트 대응

        if (runpodResponse == null || !runpodResponse.containsKey("output")) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> output = (java.util.Map<String, Object>) runpodResponse.get("output");
        String base64Image = (String) output.get("image_url");
        String description = (String) output.get("description");

        if (base64Image == null) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        // 2. Decode Base64 -> Image Bytes
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // 3. Upload to S3
        String s3FileName = "moodboards/" + UUID.randomUUID() + ".png";
        s3Template.upload(bucketName, s3FileName, new ByteArrayInputStream(imageBytes));

        String s3Url = "https://" + bucketName + ".s3.ap-southeast-2.amazonaws.com/" + s3FileName;

        // 4. Save to DB
        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId())
                .imageUrl(s3Url)
                .prompt(description)
                .build());

        return s3Url;
    }
}
