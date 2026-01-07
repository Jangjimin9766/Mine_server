package com.mine.api.controller;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.service.MagazineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "99. 내부 API (Internal) 🛠️", description = "Python 서버와의 통신을 위한 내부 API")
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final MagazineService magazineService;

    @org.springframework.beans.factory.annotation.Value("${python.api.key}")
    private String internalApiKey;

    @PostMapping("/magazine")
    public ResponseEntity<?> createMagazine(
            @RequestBody MagazineCreateRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {

        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        Long magazineId = magazineService.saveMagazine(request, request.getUserEmail());
        return ResponseEntity.ok(magazineId);
    }
}
