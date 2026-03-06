package com.mine.api.controller;

import com.mine.api.dto.SectionDto;
import com.mine.api.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/magazines/{magazineId}/sections")
@RequiredArgsConstructor
@Tag(name = "2. 섹션 편집 (수동/개별AI) 🧩", description = "각각의 문단(카드)을 수정하거나 순서를 바꿉니다. 개별 섹션의 내용을 AI로 고칠 수도 있습니다.")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/{sectionId}")
    @Operation(summary = "👁️ 섹션 상세 보기", description = "하나의 카드 내용을 상세히 봅니다.")
    public ResponseEntity<SectionDto.Response> getSection(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                sectionService.getSection(magazineId, sectionId, username));
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "🩹 섹션 삭제 (즉시)", description = "AI에게 말하지 않고 바로 지웁니다. 되돌릴 수 없습니다.")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        sectionService.deleteSection(magazineId, sectionId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sectionId}")
    @Operation(summary = "✏️ 섹션 직접 수정", description = "AI 없이 직접 본문/제목/이미지를 수정합니다. 에디터에서 수정 후 저장할 때 사용합니다.")
    public ResponseEntity<SectionDto.Response> updateSection(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @RequestBody SectionDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sectionService.updateSection(magazineId, sectionId, request, userDetails.getUsername()));
    }

    @PatchMapping("/reorder")
    @Operation(summary = "🔢 섹션 순서 바꾸기 (드래그앤드롭)", description = "드래그 앤 드롭으로 카드의 위치를 바꿉니다.")
    public ResponseEntity<Void> reorderSections(
            @PathVariable Long magazineId,
            @RequestBody @Valid SectionDto.ReorderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        sectionService.reorderSections(magazineId, request.getSectionIds(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sectionId}/interact")
    @Operation(summary = "🤖 섹션 내용 수정 (AI)", description = "AI에게 섹션 본문 수정을 요청합니다.<br>예: '더 감성적으로 바꿔줘', '길게 늘려줘', '내용 추가해줘'")
    public ResponseEntity<SectionDto.InteractResponse> interact(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @RequestBody @Valid SectionDto.InteractRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sectionService.interact(magazineId, sectionId, request.getMessage(),
                        userDetails.getUsername()));
    }
}
