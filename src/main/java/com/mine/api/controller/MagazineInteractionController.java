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

@Tag(name = "매거진 상호작용 (Magazine Interaction)", description = "AI와 대화하며 매거진을 동적으로 수정하는 API")
@RestController
@RequestMapping("/api/magazines/{magazineId}/interact")
@RequiredArgsConstructor
public class MagazineInteractionController {

    private final MagazineInteractionService interactionService;

    @Operation(summary = "매거진과 상호작용", description = "AI에게 메시지를 보내 매거진 섹션을 수정합니다. 예: '첫 번째 섹션을 더 감성적으로 바꿔줘'")
    @PostMapping
    public ResponseEntity<InteractionDto.InteractResponse> interact(
            @PathVariable Long magazineId,
            @RequestBody InteractionDto.InteractRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(interactionService.interact(magazineId, userDetails.getUsername(), request));
    }

    @Operation(summary = "상호작용 이력 조회", description = "해당 매거진의 모든 상호작용 이력을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<InteractionDto.InteractionHistory>> getHistory(
            @PathVariable Long magazineId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(interactionService.getInteractionHistory(magazineId, userDetails.getUsername()));
    }
}
