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
    private final com.mine.api.listener.MagazineGenerationListener magazineGenerationListener;
    private final com.mine.api.repository.UserRepository userRepository;
    private final com.mine.api.repository.UserInterestRepository userInterestRepository;

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

    /**
     * [복구용] 특정 유저의 초기 매거진 생성을 강제로 트리거
     */
    @PostMapping("/trigger-initial")
    public ResponseEntity<?> triggerInitial(
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String apiKey) {

        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Invalid API Key");
        }

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        java.util.List<String> interests = userInterestRepository.findByUser(user).stream()
                .map(ui -> ui.getInterest().getCode())
                .collect(java.util.stream.Collectors.toList());

        magazineGenerationListener.generateInitialMagazinesAsync(user, interests);

        return ResponseEntity.ok("Async generation triggered for user: " + username);
    }
}
