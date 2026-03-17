package com.mine.api.controller;

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

@RestController
@RequestMapping("/api/magazines")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class MagazineController {

    private final MagazineService magazineService;
    private final com.mine.api.service.MoodboardService moodboardService;

    @Tag(name = "1. 매거진 (Magazine) 📘", description = "매거진의 생성, 조회, 수정, 삭제(CRUD) 및 검색/피드 기능을 제공합니다.")
    @Operation(summary = "📂 내 매거진 목록", description = "내가 만든 매거진들을 최신순으로 모아봅니다.")
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>> getMyMagazines(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.data.web.SortDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {

        return ResponseEntity.ok(magazineService.getMyMagazinesPage(userDetails.getUsername(), pageable));
    }

    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "📖 매거진 상세 보기", description = "매거진의 모든 내용(섹션 포함)을 상세하게 봅니다. 모든 카드가 순서대로 보여집니다.")
    @GetMapping("/{id}")
    public ResponseEntity<com.mine.api.dto.MagazineDto.DetailResponse> getMagazineDetail(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String viewerUsername = userDetails != null ? userDetails.getUsername() : null;
        com.mine.api.dto.MagazineDto.DetailResponse response = magazineService.getMagazineDetail(id,
                viewerUsername);
        return ResponseEntity.ok(response);
    }

    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "✨ AI 매거진 만들기", description = "주제와 기분을 입력하면 AI가 제목, 소개, 그리고 내용(섹션)까지 자동으로 만들어줍니다. (로컬 생성 시 1~2분 이상 소요될 수 있습니다)")
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<Long> createMagazine(
            @org.springframework.web.bind.annotation.RequestBody com.mine.api.dto.MagazineGenerationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long magazineId = magazineService.generateAndSaveMagazine(request, userDetails.getUsername());
        return ResponseEntity.ok(magazineId);
    }

    // ⭐ Phase 1: 매거진 삭제
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "🗑️ 매거진 삭제", description = "매거진을 완전히 삭제합니다. 되돌릴 수 없습니다.")
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
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "✏️ 제목/소개 수정", description = "매거진의 이름이나 소개글만 살짝 고칩니다. (내용 수정은 섹션 API 사용)")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    public ResponseEntity<?> updateMagazine(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 제목/소개", required = true, content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.mine.api.dto.MagazineDto.UpdateRequest.class))) @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.mine.api.dto.MagazineDto.UpdateRequest request,
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
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "❤️ 좋아요 누르기/취소", description = "마음에 드는 매거진에 좋아요를 누르거나 취소합니다.")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/likes")
    public ResponseEntity<java.util.Map<String, Object>> toggleLike(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        boolean liked = magazineService.toggleLike(id, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of("liked", liked));
    }

    // ⭐ Phase 3: 내가 좋아요한 매거진 목록
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "💘 내가 찜한 매거진", description = "내가 좋아요를 누른 매거진들을 모아봅니다.")
    @GetMapping("/liked")
    public ResponseEntity<org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>> getLikedMagazines(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @org.springframework.data.web.SortDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {

        return ResponseEntity.ok(magazineService.getLikedMagazines(userDetails.getUsername(), pageable));
    }

    // ⭐ 커버 이미지 변경
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "🖼️ 커버 이미지 변경", description = "매거진 커버 이미지를 변경합니다. 무드보드 이미지 URL을 사용하세요.")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/cover")
    public ResponseEntity<?> updateCover(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "새 커버 이미지 URL", required = true, content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"coverImageUrl\": \"https://your-bucket.s3.amazonaws.com/moodboard.png\"}"))) @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String newCoverUrl = request.get("coverImageUrl");
            if (newCoverUrl == null || newCoverUrl.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "coverImageUrl is required"));
            }

            magazineService.updateCover(id, newCoverUrl, userDetails.getUsername());
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "커버 이미지가 변경되었습니다",
                    "coverImageUrl", newCoverUrl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "권한이 없습니다"));
        }
    }

    // ⭐ 공개 매거진 목록 조회 (인증 불필요)
    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "🌍 공개 매거진 목록", description = "공개된 계정의 모든 매거진을 ID순으로 조회합니다. 로그인 없이 볼 수 있습니다.")
    @GetMapping("/public")
    public ResponseEntity<org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>> getPublicMagazines(
            @io.swagger.v3.oas.annotations.Parameter(description = "특정 유저 ID (선택)", required = false) @org.springframework.web.bind.annotation.RequestParam(required = false) Long userId,
            @org.springframework.data.web.SortDefault(sort = "id", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(magazineService.getPublicMagazines(userId, pageable));
    }

    // ⭐ Phase 2: 검색 (키워드)
    @Tag(name = "99. 기타 (보류) 💤")
    @Operation(summary = "🔍 매거진 검색", description = "키워드로 원하는 매거진을 찾습니다.")
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @org.springframework.web.bind.annotation.RequestParam String keyword,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "검색어를 입력해주세요"));
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        String username = userDetails != null ? userDetails.getUsername() : "";
        org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> result = magazineService
                .searchByKeyword(keyword, username, pageable);

        return ResponseEntity.ok(result);
    }

    @Tag(name = "1. 매거진 (Magazine) 📘")
    @Operation(summary = "📡 추천 피드 (커서 기반)", description = "내 관심사와 팔로우한 사람들의 새 글을 모아서 보여줍니다. (무한 스크롤) ?isTest=true 시 모든 공개글을 최신순으로 반환합니다.")
    @GetMapping("/feed")
    public ResponseEntity<com.mine.api.dto.CursorResponse<com.mine.api.dto.MagazineDto.ListItem>> getPersonalizedFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @io.swagger.v3.oas.annotations.Parameter(description = "마지막으로 조회한 매거진 ID (첫 조회 시 null)") @org.springframework.web.bind.annotation.RequestParam(required = false) Long cursorId,
            @io.swagger.v3.oas.annotations.Parameter(description = "한 번에 가져올 개수") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit,
            @io.swagger.v3.oas.annotations.Parameter(description = "테스트 모드 여부 (true 시 전체 공개글 최신순)") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean isTest) {

        return ResponseEntity.ok(magazineService.getPersonalizedFeedCursor(userDetails.getUsername(), cursorId, limit, isTest));
    }

    // ⭐ 매거진 기반 무드보드 생성
    @Tag(name = "99. 기타 (보류) 💤")
    @Operation(summary = "🎨 매거진 무드보드 생성", description = "매거진 정보를 기반으로 AI 무드보드를 생성합니다. 매거진 제목과 태그가 자동으로 사용됩니다.")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/moodboards")
    public ResponseEntity<?> createMoodboardForMagazine(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String imageUrl = moodboardService.createMoodboardForMagazine(id, userDetails.getUsername());

            com.mine.api.dto.MoodboardResponseDto response = com.mine.api.dto.MoodboardResponseDto.builder()
                    .image_url(imageUrl)
                    .description("Moodboard generated for magazine #" + id)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "무드보드 생성 권한이 없습니다")); // 403 Forbidden

        } catch (Exception e) {
            log.error("Failed to create moodboard for magazine {}: {}", id, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "무드보드 생성 실패: " + e.getMessage())); // 500
        }
    }

    // ⭐ 무드보드 히스토리 조회
    @Tag(name = "99. 기타 (보류) 💤")
    @Operation(summary = "📜 무드보드 히스토리", description = "매거진에서 생성한 무드보드 목록을 최신순으로 조회합니다.")
    @GetMapping("/{id}/moodboards/history")
    public ResponseEntity<?> getMoodboardHistory(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            java.util.List<com.mine.api.domain.Moodboard> history = moodboardService.getMoodboardHistory(id,
                    userDetails.getUsername());

            // 간단한 응답 형태로 변환 (id, imageUrl, createdAt)
            java.util.List<java.util.Map<String, Object>> response = history.stream()
                    .map(m -> java.util.Map.<String, Object>of(
                            "id", m.getId(),
                            "imageUrl", m.getImageUrl(),
                            "description", m.getPrompt() != null ? m.getPrompt() : "",
                            "createdAt", m.getCreatedAt().toString()))
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "무드보드 히스토리 조회 권한이 없습니다"));
        }
    }
}
