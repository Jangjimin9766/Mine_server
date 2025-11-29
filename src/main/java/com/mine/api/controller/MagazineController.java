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
@lombok.extern.slf4j.Slf4j
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

    // ⭐ Phase 1: 매거진 삭제
    @Operation(summary = "매거진 삭제", description = "매거진을 삭제합니다. 본인의 매거진만 삭제 가능합니다.")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMagazine(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            magazineService.deleteMagazine(id, userDetails.getUsername());
            return ResponseEntity.noContent().build(); // 204 No Content

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "삭제 권한이 없습니다")); // 403 Forbidden

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "서버 오류: " + e.getMessage())); // 500
        }
    }

    // ⭐ Phase 1: 매거진 수정
    @Operation(summary = "매거진 수정", description = "매거진 제목/소개를 수정합니다. 본인의 매거진만 수정 가능합니다.")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    public ResponseEntity<?> updateMagazine(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.mine.api.dto.MagazineDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            magazineService.updateMagazine(id, request, userDetails.getUsername());
            return ResponseEntity.ok(java.util.Map.of("message", "수정되었습니다")); // 200 OK

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage())); // 400 Bad Request

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "수정 권한이 없습니다")); // 403 Forbidden

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "서버 오류: " + e.getMessage())); // 500
        }
    }

    // ⭐ Phase 3: 좋아요 토글
    @Operation(summary = "매거진 좋아요", description = "매거진에 좋아요를 하거나 취소합니다.")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/likes")
    public ResponseEntity<java.util.Map<String, Object>> toggleLike(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        boolean liked = magazineService.toggleLike(id, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of("liked", liked));
    }

    // ⭐ Phase 3: 내가 좋아요한 매거진 목록
    @Operation(summary = "좋아요한 매거진 목록", description = "내가 좋아요한 매거진 목록을 조회합니다.")
    @GetMapping("/liked")
    public ResponseEntity<org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>> getLikedMagazines(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @org.springframework.data.web.SortDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {

        return ResponseEntity.ok(magazineService.getLikedMagazines(userDetails.getUsername(), pageable));
    }

    // ⭐ Phase 2: 공개/비공개 설정
    @Operation(summary = "공개/비공개 설정", description = "매거진을 공개하거나 비공개로 설정합니다. 공개 시 공유 링크가 생성됩니다.")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/visibility")
    public ResponseEntity<?> setVisibility(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.mine.api.dto.MagazineDto.VisibilityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            com.mine.api.dto.MagazineDto.VisibilityResponse response = magazineService.setVisibility(
                    id, request.getIsPublic(), userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "권한이 없습니다"));
        }
    }

    // ⭐ Phase 2: 공유 링크로 조회 (인증 불필요)
    @Operation(summary = "공유 링크로 조회", description = "공유 토큰으로 매거진을 조회합니다. 인증이 필요 없습니다.")
    @GetMapping("/share/{shareToken}")
    public ResponseEntity<?> getByShareToken(@org.springframework.web.bind.annotation.PathVariable String shareToken) {
        try {
            Magazine magazine = magazineService.getByShareToken(shareToken);
            return ResponseEntity.ok(magazine);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "비공개 매거진입니다"));
        }
    }

    // ⭐ Phase 2: 검색 (키워드)
    @Operation(summary = "매거진 검색", description = "키워드로 공개 매거진을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @org.springframework.web.bind.annotation.RequestParam String keyword,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "검색어를 입력해주세요"));
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Magazine> result = magazineService.searchByKeyword(keyword, pageable);

        return ResponseEntity.ok(result);
    }

    // ⭐ Phase 2: 내 매거진 조회
    @Operation(summary = "내 매거진 조회", description = "로그인한 사용자의 모든 매거진을 페이징하여 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<?> getMyMagazinesPage(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        org.springframework.data.domain.Page<Magazine> magazines = magazineService.getMyMagazinesPage(
                userDetails.getUsername(), pageable);

        return ResponseEntity.ok(magazines);
    }
}
