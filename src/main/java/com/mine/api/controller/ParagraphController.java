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

import java.util.Map;

@RestController
@RequestMapping("/api/magazines/{magazineId}/sections/{sectionId}/paragraphs")
@RequiredArgsConstructor
@Tag(name = "2. 문단 (Paragraph) 📝", description = "매거진 섹션 내 문단(이미지+텍스트)을 관리합니다.")
public class ParagraphController {

    private final ParagraphService paragraphService;

    @Operation(summary = "✏️ 문단 수정", description = "특정 문단의 내용(소제목, 본문, 이미지)을 수정합니다.")
    @PutMapping("/{paragraphId}")
    public ResponseEntity<?> updateParagraph(
            @PathVariable Long magazineId,
            @PathVariable Long sectionId,
            @PathVariable Long paragraphId,
            @RequestBody ParagraphDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        paragraphService.updateParagraph(magazineId, sectionId, paragraphId, request, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "문단이 수정되었습니다."));
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
