package com.mine.api.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public String uploadImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "image.jpg";
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // 파일을 'uploads/' 경로에 저장
        String key = "uploads/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, key, inputStream);
        }

        // S3 URL 반환
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * 외부 URL 이미지를 다운로드하여 S3에 업로드
     */
    public String uploadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "https://mine-moodboard-bucket.s3.ap-southeast-2.amazonaws.com/assets/default-placeholder.png";
        }

        // 이미 내 S3에 있는 이미지라면 패스
        if (imageUrl.contains(".amazonaws.com") && imageUrl.contains(bucketName)) {
            return imageUrl;
        }

        try {
            java.net.URL url = new java.net.URL(imageUrl);
            String extension = ".jpg"; // 기본 확장자

            // URL 경로에서 확장자 추출 시도
            String path = url.getPath();
            int dotIndex = path.lastIndexOf(".");
            if (dotIndex >= 0) {
                String ext = path.substring(dotIndex);
                if (ext.matches("(?i)\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                    extension = ext;
                }
            }

            String key = "uploads/" + UUID.randomUUID().toString() + extension;

            java.net.URLConnection connection = url.openConnection();
            // 자바 봇이 아닌 일반 다운로더처럼 위장
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            try (InputStream inputStream = connection.getInputStream()) {
                s3Template.upload(bucketName, key, inputStream);
            }

            // S3 URL 반환
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            log.info("Uploaded external image to S3: {} -> {}", imageUrl, s3Url);
            return s3Url;

        } catch (Exception e) {
            log.error("Failed to upload image from URL: {}", imageUrl, e);
            // 실패 시 기본 이미지 반환 (또는 원본 URL 유지)
            // 여기서는 "기본 이미지"를 반환하도록 수정하여 엑스박스를 방지합니다.
            return "https://mine-moodboard-bucket.s3.ap-southeast-2.amazonaws.com/assets/default-placeholder.png";
        }
    }

    /**
     * Base64 이미지 S3 업로드
     */
    public String uploadBase64ToS3(String base64Image) {
        if (base64Image == null)
            return null;

        // 이미 URL인 경우 그대로 반환
        if (base64Image.startsWith("http")) {
            return base64Image;
        }

        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

        String s3FileName = "moodboards/" + UUID.randomUUID() + ".png";
        s3Template.upload(bucketName, s3FileName, new java.io.ByteArrayInputStream(imageBytes));

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3FileName);
    }
}
