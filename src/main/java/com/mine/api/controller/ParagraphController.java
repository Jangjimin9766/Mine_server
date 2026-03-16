package com.mine.api.controller;

import com.mine.api.dto.ParagraphDto;
import com.mine.api.service.ParagraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/magazines/{magazineId}/sections/{sectionId}/paragraphs")
@RequiredArgsConstructor
@Tag(name = "2. 문단 (Paragraph) 📝", description = "매거진 섹션 내 문단(이미지+텍스트)을 관리합니다.")
public class ParagraphController {

    private final ParagraphService paragraphService;

    @Operation(summary = "➕ 문단 추가", description = "섹션의 맨 마지막에 새로운 문단을 추가합니다.")
    @PostMapping
    public ResponseEntity<Long> createParagraph(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @RequestBody ParagraphDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long paragraphId = paragraphService.createParagraph(magazineId, sectionId, request, userDetails.getUsername());
        return ResponseEntity.ok(paragraphId);
    }

    @Operation(summary = "✨ AI 문단 추가", description = "AI를 사용하여 섹션의 맨 마지막에 새로운 문단을 생성하고 추가합니다.")
    @PostMapping("/ai")
    public ResponseEntity<Long> createParagraphWithAi(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @RequestBody ParagraphDto.AiCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long paragraphId = paragraphService.createParagraphWithAi(magazineId, sectionId, request, userDetails.getUsername());
        return ResponseEntity.ok(paragraphId);
    }

    @Operation(summary = "🗑️ 문단 삭제", description = "특정 문단을 삭제합니다.")
    @DeleteMapping("/{paragraphId}")
    public ResponseEntity<?> deleteParagraph(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @PathVariable Long paragraphId,
            @AuthenticationPrincipal UserDetails userDetails) {

        paragraphService.deleteParagraph(magazineId, sectionId, paragraphId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
