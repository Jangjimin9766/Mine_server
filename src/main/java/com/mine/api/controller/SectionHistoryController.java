package com.mine.api.controller;

import com.mine.api.dto.SectionViewHistoryDto;
import com.mine.api.service.SectionViewHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
@Tag(name = "3. 섹션 열람 기록 📜", description = "최근 열람한 섹션 히스토리를 조회합니다.")
public class SectionHistoryController {

    private final SectionViewHistoryService sectionViewHistoryService;

    @GetMapping("/recent")
    @Operation(summary = "📜 최근 열람 섹션 목록", description = "최근 한 달 내 열람한 섹션 목록을 최신순으로 반환합니다. (최대 30개)")
    public ResponseEntity<List<SectionViewHistoryDto.Response>> getRecentSections(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sectionViewHistoryService.getRecentViews(userDetails.getUsername()));
    }
}
