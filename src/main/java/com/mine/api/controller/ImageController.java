package com.mine.api.controller;

import com.mine.api.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Tag(name = "6. 이미지 (Image) 🖼️", description = "이미지 업로드 API (프로필, 매거진 등)")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @Operation(summary = "이미지 업로드", description = "이미지 파일을 업로드하고 URL을 반환받습니다. 반환된 URL을 프로필 수정이나 매거진 생성 시 사용하세요.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestPart("file") MultipartFile file) {

        try {
            String imageUrl = s3Service.uploadImage(file);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
        }
    }
}
