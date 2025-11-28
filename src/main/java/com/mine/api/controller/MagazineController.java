package com.mine.api.controller;

import com.mine.api.domain.Magazine;
import com.mine.api.service.MagazineService;
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

@Tag(name = "매거진 (Magazine)", description = "매거진 생성 및 조회 API")
@RestController
@RequestMapping("/api/magazines")
@RequiredArgsConstructor
public class MagazineController {

    private final MagazineService magazineService;

    @Operation(summary = "내 매거진 목록 조회", description = "로그인한 사용자가 생성한 모든 매거진을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<Magazine>> getMyMagazines(@AuthenticationPrincipal UserDetails userDetails) {
        List<Magazine> magazines = magazineService.getMagazinesByUser(userDetails.getUsername());
        return ResponseEntity.ok(magazines);
    }

    @Operation(summary = "매거진 상세 조회", description = "특정 매거진의 상세 정보를 조회합니다. 본인의 매거진만 조회 가능합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<Magazine> getMagazineDetail(@org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Magazine magazine = magazineService.getMagazineDetail(id, userDetails.getUsername());
        return ResponseEntity.ok(magazine);
    }

    @Operation(summary = "매거진 생성 (AI)", description = "주제와 기분을 입력하면 AI가 매거진을 자동 생성합니다. Python 서버와 연동됩니다.")
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<Long> createMagazine(
            @org.springframework.web.bind.annotation.RequestBody com.mine.api.dto.MagazineGenerationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long magazineId = magazineService.generateAndSaveMagazine(request, userDetails.getUsername());
        return ResponseEntity.ok(magazineId);
    }
}
