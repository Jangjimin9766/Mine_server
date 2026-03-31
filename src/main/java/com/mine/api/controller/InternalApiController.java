package com.mine.api.controller;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.service.MagazineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "99. 내부 API (Internal) 🛠️", description = "Python 서버와의 통신을 위한 내부 API")
@RestController
@RequestMapping("/api/internal")
@Slf4j
public class InternalApiController {

    private final MagazineService magazineService;
    private final com.mine.api.service.S3Service s3Service;

    @org.springframework.beans.factory.annotation.Value("${mine.internal.secret-key:mine-admin-1234}")
    private String internalApiKey;

    public InternalApiController(MagazineService magazineService, com.mine.api.service.S3Service s3Service) {
        this.magazineService = magazineService;
        this.s3Service = s3Service;
    }

    @PostMapping("/magazine")
    public ResponseEntity<?> createMagazine(
            @RequestBody MagazineCreateRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {

        // JWT 없이 X-Internal-Key 헤더로 인증 — Python AI 서버가 직접 호출할 때 사용
        if (internalApiKey == null || !internalApiKey.trim().equals(apiKey.trim())) {
            log.warn("Invalid API Key for /magazine. Expected length: {}, Provided length: {}", 
                    (internalApiKey != null ? internalApiKey.trim().length() : "null"), 
                    (apiKey != null ? apiKey.trim().length() : "null"));
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        Long magazineId = magazineService.saveMagazine(request, request.getUserEmail());
        return ResponseEntity.ok(magazineId);
    }

    /**
     * [복구용] 특정 유저의 초기 매거진 생성을 강제로 트리거
     */
    @PostMapping("/trigger-initial")
    public ResponseEntity<?> triggerInitial(
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {

        if (internalApiKey == null || !internalApiKey.trim().equals(apiKey.trim())) {
            log.warn("Invalid API Key for /trigger-initial. Expected length: {}, Provided length: {}", 
                    (internalApiKey != null ? internalApiKey.trim().length() : "null"), 
                    (apiKey != null ? apiKey.trim().length() : "null"));
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        // 비동기로 실행 — username만 넘겨서 Lazy loading/Detached entity 문제 방지
        magazineService.generateInitialMagazinesAsync(username);

        return ResponseEntity.ok("Async generation triggered for user: " + username);
    }
    @org.springframework.web.bind.annotation.PostMapping("/init-assets")
    public ResponseEntity<String> initAssets(
            @org.springframework.web.bind.annotation.RequestParam String fileName,
            @org.springframework.web.bind.annotation.RequestBody String base64Data,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {
        
        if (internalApiKey == null || !internalApiKey.trim().equals(apiKey.trim())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        try {
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            s3Service.initializeStaticAssets(imageBytes, fileName);
            return ResponseEntity.ok("Successfully initialized asset: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to initialize asset: " + e.getMessage());
        }
    }
}
