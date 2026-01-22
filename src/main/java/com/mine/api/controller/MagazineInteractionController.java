package com.mine.api.controller;

import com.mine.api.dto.InteractionDto;
import com.mine.api.service.MagazineInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. 매거진 AI 편집 (전체/구조) 🤖", description = "AI와 대화하며 섹션을 추가하거나 삭제하고, 전체 분위기를 바꿉니다.")
@RestController
@RequestMapping("/api/magazines/{magazineId}/interact")
@RequiredArgsConstructor
public class MagazineInteractionController {

    private final MagazineInteractionService interactionService;

    @Operation(summary = "🤖 섹션 추가 (AI)", description = "AI에게 새 섹션 추가를 요청합니다.<br>예: '여행 섹션 추가해줘', '디저트 소개 추가해줘'")
    @PostMapping
    public ResponseEntity<InteractionDto.InteractResponse> interact(
            @PathVariable Long magazineId,
            @RequestBody InteractionDto.InteractRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(interactionService.interact(magazineId, userDetails.getUsername(), request));
    }

    @Operation(summary = "📜 AI 대화 이력", description = "이 매거진에서 AI와 나눴던 대화 목록을 봅니다.")
    @GetMapping
    public ResponseEntity<List<InteractionDto.InteractionHistory>> getHistory(
            @PathVariable Long magazineId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(interactionService.getInteractionHistory(magazineId, userDetails.getUsername()));
    }
}
