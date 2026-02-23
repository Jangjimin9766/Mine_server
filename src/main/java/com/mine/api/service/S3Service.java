package com.mine.api.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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

    private final S3Client s3Client;

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

        String contentType = file.getContentType();
        if (contentType == null)
            contentType = "image/jpeg";

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
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

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg";
                if (extension.equalsIgnoreCase(".png"))
                    contentType = "image/png";
                else if (extension.equalsIgnoreCase(".gif"))
                    contentType = "image/gif";
                else if (extension.equalsIgnoreCase(".webp"))
                    contentType = "image/webp";
            }

            try (InputStream inputStream = connection.getInputStream()) {
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();
                long contentLength = connection.getContentLengthLong();
                if (contentLength > 0) {
                    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
                } else {
                    byte[] imageBytes = inputStream.readAllBytes();
                    s3Client.putObject(request, RequestBody.fromBytes(imageBytes));
                }
            }

            // S3 URL 반환
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            log.info("Uploaded external image to S3: {} -> {}", imageUrl, s3Url);
            return s3Url;

        } catch (Exception e) {
            log.error("Failed to upload image from URL: {}", imageUrl, e);
            // 실패 시 원본 이미지 URL이라도 그대로 반환하여 클라이언트에서 보이게 함
            return imageUrl;
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
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3FileName)
                .contentType("image/png")
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(imageBytes));

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3FileName);
    }
}
