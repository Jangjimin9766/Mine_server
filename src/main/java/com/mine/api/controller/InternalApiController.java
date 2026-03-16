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
    private final com.mine.api.listener.MagazineGenerationListener magazineGenerationListener;
    private final String internalApiKey;

    public InternalApiController(
            MagazineService magazineService,
            com.mine.api.listener.MagazineGenerationListener magazineGenerationListener,
            @org.springframework.beans.factory.annotation.Value("${python.api.key}") String internalApiKey) {
        this.magazineService = magazineService;
        this.magazineGenerationListener = magazineGenerationListener;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping("/magazine")
    public ResponseEntity<?> createMagazine(
            @RequestBody MagazineCreateRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {

        if (internalApiKey == null || !internalApiKey.equals(apiKey)) {
            log.warn("Invalid API Key attempt for /magazine");
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

        if (internalApiKey == null || !internalApiKey.equals(apiKey)) {
            log.warn("Invalid API Key attempt for /trigger-initial");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        // 비동기 생성을 위해 username만 넘김 (Lazy loading 및 Detached entity 방지)
        magazineGenerationListener.generateInitialMagazinesAsync(username);

        return ResponseEntity.ok("Async generation triggered for user: " + username);
    }
}
