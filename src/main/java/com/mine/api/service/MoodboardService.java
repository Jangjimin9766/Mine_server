package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.dto.MoodboardResponseDto;
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

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public String createMoodboard(String username, MoodboardRequestDto requestDto) {
        // 0. Find User
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Call Python AI Server
        // Increase buffer size to handle large Base64 images (default is 256KB)
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        MoodboardResponseDto aiResponse = webClientBuilder.exchangeStrategies(strategies).build()
                .post()
                .uri(moodboardApiUrl)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(MoodboardResponseDto.class)
                .block(); // Blocking for simplicity in this MVP, consider reactive stack for high load

        if (aiResponse == null || aiResponse.getImage_url() == null) {
            throw new RuntimeException("Failed to generate moodboard image");
        }

        // 2. Decode Base64 -> Image Bytes
        String base64Image = aiResponse.getImage_url();
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // 3. Upload to S3
        String s3FileName = "moodboards/" + UUID.randomUUID() + ".png";
        s3Template.upload(bucketName, s3FileName, new ByteArrayInputStream(imageBytes));

        // Construct S3 URL (assuming standard AWS S3 URL format)
        // Note: S3Template doesn't return the full URL directly, so we construct it or
        // use S3Utilities if available
        // For now, let's construct it manually or fetch from a utility if we had one.
        // A robust way is to get the URL from the resource, but S3Template returns
        // S3Resource.
        // Let's use a simple format for now:
        // https://{bucket}.s3.{region}.amazonaws.com/{key}
        // Or better, just return the key if the frontend constructs the URL, but the
        // requirement says return URL.
        // Let's assume a standard public URL for this MVP.
        String s3Url = "https://" + bucketName + ".s3.ap-southeast-2.amazonaws.com/" + s3FileName;

        // 4. Save to DB
        moodboardRepository.save(Moodboard.builder()
                .userId(user.getId()) // Changed from userId to user.getId()
                .imageUrl(s3Url)
                .prompt(aiResponse.getDescription())
                .build());

        return s3Url;
    }
}
