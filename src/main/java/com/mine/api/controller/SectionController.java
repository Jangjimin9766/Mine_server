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
@Tag(name = "Section", description = "섹션 관리 API")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/{sectionId}")
    @Operation(summary = "섹션 상세 조회", description = "매거진 내 특정 섹션의 상세 정보를 조회합니다.")
    public ResponseEntity<SectionDto.Response> getSection(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sectionService.getSection(magazineId, sectionId, userDetails.getUsername()));
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "섹션 삭제", description = "매거진 내 특정 섹션을 삭제합니다. AI 프롬프트 없이 직접 삭제됩니다.")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        sectionService.deleteSection(magazineId, sectionId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    @Operation(summary = "섹션 순서 변경", description = "드래그 앤 드롭으로 섹션 순서를 변경합니다.")
    public ResponseEntity<Void> reorderSections(
            @PathVariable Long magazineId,
            @RequestBody @Valid SectionDto.ReorderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        sectionService.reorderSections(magazineId, request.getSectionIds(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sectionId}/interact")
    @Operation(summary = "섹션 AI 상호작용", description = "AI 프롬프트로 섹션 본문을 수정합니다.")
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
