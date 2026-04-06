package com.mine.api.service;

import com.mine.api.common.ErrorMessages;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.Paragraph;
import com.mine.api.domain.User;
import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.UserRepository;
import com.mine.api.repository.MagazineLikeRepository;
import com.mine.api.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final UserRepository userRepository;
    private final MagazineLikeRepository magazineLikeRepository;
    private final UserInterestRepository userInterestRepository;
    private final RunPodService runPodService;
    private final MoodboardService moodboardService;
    private final com.mine.api.repository.MoodboardRepository moodboardRepository;
    private final S3Service s3Service;
    private final SectionService sectionService;

    @org.springframework.beans.factory.annotation.Value("${python.api.url}")
    private String pythonApiUrl;

    @org.springframework.beans.factory.annotation.Value("${python.api.key}")
    private String pythonApiKey;

    @Transactional
    public Long saveMagazine(MagazineCreateRequest request, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        // 1. 태그 리스트를 JSON 문자열로 변환
        String tagsJson = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagsJson = String.join(",", request.getTags());
        }

        // 2. 무드보드 정보 추출 (이미 있으면 S3 업로드)
        String moodboardImageUrl = null;
        if (request.getMoodboard() != null) {
            moodboardImageUrl = request.getMoodboard().getImage_url();

            if (moodboardImageUrl != null
                    && (moodboardImageUrl.startsWith("data:image") || moodboardImageUrl.length() > 255)) {
                moodboardImageUrl = s3Service.uploadBase64ToS3(moodboardImageUrl);
            }
        }

        // 3. 섹션 썸네일/문단 이미지를 CompletableFuture로 병렬 S3 업로드 — 순차 업로드 대비 속도 대폭 개선
        java.util.List<java.util.concurrent.CompletableFuture<Void>> uploadTasks = new java.util.ArrayList<>();

        if (request.getSections() != null) {
            for (MagazineCreateRequest.SectionDto sectionDto : request.getSections()) {
                String originalUrl = sectionDto.getThumbnailUrl();
                if (originalUrl != null && !originalUrl.isBlank()) {
                    uploadTasks.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                        String uploadedUrl = s3Service.uploadImageFromUrl(originalUrl);
                        if (uploadedUrl != null) {
                            sectionDto.setThumbnailUrl(uploadedUrl);
                        }
                    }));
                }

                if (sectionDto.getParagraphs() != null) {
                    for (MagazineCreateRequest.ParagraphDto paraDto : sectionDto.getParagraphs()) {
                        String pUrl = paraDto.getImageUrl();
                        if (pUrl != null && !pUrl.isBlank()) {
                            uploadTasks.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                                String uploadedUrl = s3Service.uploadImageFromUrl(pUrl);
                                if (uploadedUrl != null) {
                                    paraDto.setImageUrl(uploadedUrl);
                                }
                            }));
                        }
                    }
                }
            }
        }

        // 업로드 실패는 경고만 남기고 진행 — 이미지 없이 저장하는 게 저장 실패보다 낫다
        if (!uploadTasks.isEmpty()) {
            try {
                java.util.concurrent.CompletableFuture.allOf(uploadTasks.toArray(new java.util.concurrent.CompletableFuture[0]))
                        .get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Some image uploads timed out or failed, proceeding with available URLs", e);
            }
        }

        // 4. Magazine 엔티티 생성
        String coverImageUrl = moodboardImageUrl != null ? moodboardImageUrl : request.getCoverImageUrl();

        Magazine magazine = Magazine.builder()
                .title(truncate(request.getTitle(), 490))
                .coverImageUrl(truncate(coverImageUrl, 990))
                .tags(tagsJson)
                .moodboardImageUrl(truncate(moodboardImageUrl, 990))
                .user(user)
                .build();

        // 5. Section/Paragraph 엔티티 생성 및 저장
        if (request.getSections() != null) {
            for (int i = 0; i < request.getSections().size(); i++) {
                MagazineCreateRequest.SectionDto sectionDto = request.getSections().get(i);

                MagazineSection section = MagazineSection.builder()
                        .heading(truncate(sectionDto.getHeading(), 490))
                        .thumbnailUrl(truncate(sectionDto.getThumbnailUrl(), 990))
                        .displayOrder(i)
                        .sourceUrl(sectionDto.getSourceUrl()) // 원본 웹 소스 URL 저장
                        .build();

                String firstParaImageUrl = null;
                if (sectionDto.getParagraphs() != null) {
                    for (int j = 0; j < sectionDto.getParagraphs().size(); j++) {
                        MagazineCreateRequest.ParagraphDto paraDto = sectionDto.getParagraphs().get(j);
                        
                        if (firstParaImageUrl == null && paraDto.getImageUrl() != null) {
                            firstParaImageUrl = paraDto.getImageUrl();
                        }

                        Paragraph paragraph = Paragraph.builder()
                                .subtitle(truncate(paraDto.getSubtitle() != null && !paraDto.getSubtitle().isEmpty() ? paraDto.getSubtitle() : "소제목 내용", 490))
                                .text(paraDto.getText() != null && !paraDto.getText().isEmpty() ? paraDto.getText() : "내용을 입력해주세요.")
                                .imageUrl(truncate(paraDto.getImageUrl(), 990))
                                .sourceUrl(truncate(paraDto.getSourceUrl(), 1990))
                                .displayOrder(j)
                                .build();
                        section.addParagraph(paragraph);
                    }
                }

                // 썸네일이 없으면 첫 번째 문단 이미지를 대신 사용 — 빈 썸네일 방지
                if (section.getThumbnailUrl() == null || section.getThumbnailUrl().startsWith("http")) {
                    if (firstParaImageUrl != null) {
                        section.setThumbnailUrl(firstParaImageUrl);
                    }
                }

                // 그래도 없으면 기본 플레이스홀더 이미지 사용
                if (section.getThumbnailUrl() == null) {
                    section.setThumbnailUrl("https://mine-moodboard-bucket.s3.ap-southeast-2.amazonaws.com/assets/default-placeholder.png");
                }

                magazine.addSection(section);
            }
        }

        // 5. 저장 (CascadeType.ALL로 인해 Section도 함께 저장됨)
        log.info("Saving magazine entity: {}", magazine.getTitle());
        Magazine savedMagazine;
        try {
            savedMagazine = magazineRepository.save(magazine);
        } catch (Exception e) {
            log.error("Failed to save magazine entity to DB", e);
            throw new RuntimeException("매거진 데이터베이스 저장 실패: " + e.getMessage());
        }

        // 6. 무드보드가 있으면 moodboards 테이블에도 저장 (히스토리 용)
        if (moodboardImageUrl != null && request.getMoodboard() != null && request.getMoodboard().getDescription() != null) {
            com.mine.api.domain.Moodboard moodboard = com.mine.api.domain.Moodboard.builder()
                    .userId(user.getId())
                    .magazineId(savedMagazine.getId())
                    .imageUrl(moodboardImageUrl)
                    .prompt(request.getMoodboard().getDescription())
                    .build();
            moodboardRepository.save(moodboard);
        }

        return savedMagazine.getId();
    }

    private String truncate(String str, int length) {
        if (str == null) return null;
        return str.length() > length ? str.substring(0, length) : str;
    }

    /**
     * N+1 쿼리 방지: fetch join으로 sections와 user를 한 번에 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<Magazine> getMagazinesByUser(String username) {
        return magazineRepository.findByUserUsernameWithSections(username);
    }

    @Transactional(readOnly = true)
    public com.mine.api.dto.MagazineDto.DetailResponse getMagazineDetail(Long id, String username) {
        Magazine magazine = magazineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        // 본인 매거진이 아니고 비공개 계정이면 접근 불가
        boolean isOwner = username != null && magazine.getUser().getUsername().equals(username);
        boolean isPublicAccount = magazine.getUser().getIsPublic();

        if (!isOwner && !isPublicAccount) {
            throw new SecurityException(ErrorMessages.PRIVATE_ACCOUNT);
        }

        // 좋아요 여부 조회 (비로그인 시 isLiked = false)
        boolean isLiked = false;
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                isLiked = magazineLikeRepository.existsByUserAndMagazine(user, magazine);
            }
        }

        // displayOrder 순으로 섹션 정렬
        magazine.getSections().sort((s1, s2) -> {
            Integer o1 = s1.getDisplayOrder() != null ? s1.getDisplayOrder() : Integer.MAX_VALUE;
            Integer o2 = s2.getDisplayOrder() != null ? s2.getDisplayOrder() : Integer.MAX_VALUE;
            return o1.compareTo(o2);
        });

        return com.mine.api.dto.MagazineDto.DetailResponse.from(magazine, isLiked);
    }

    public Long generateAndSaveMagazine(com.mine.api.dto.MagazineGenerationRequest request, String username) {
        try {
            // 1. 사용자 관심사 조회
            com.mine.api.domain.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            java.util.List<String> userInterests = userInterestRepository.findByUser(user).stream()
                    .map(ui -> ui.getInterest().getCode())
                    .collect(java.util.stream.Collectors.toList());

            // 2. Python 서버 요청 준비
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put(com.mine.api.common.AppConstants.KEY_TOPIC, request.getTopic());
            data.put(com.mine.api.common.AppConstants.KEY_USER_MOOD, request.getUserMood());
            data.put(com.mine.api.common.AppConstants.KEY_USER_EMAIL, username);
            data.put("user_interests", userInterests);

            java.util.Map<String, Object> responseBody;

            // 3. 호출 방식 분기 (Local vs RunPod)
            if (pythonApiUrl.contains("localhost") || pythonApiUrl.contains("127.0.0.1")) {
                // Local FastAPI 호출 (직접 전송, 동기식)
                data.put("action", "create_magazine"); // 로컬에서도 action 필수
                responseBody = runPodService.sendSyncRequest(pythonApiUrl, data);
            } else {
                // RunPod Serverless 호출 (input 래핑, 비동기 폴링)
                // { "input": { "action": "create_magazine", "data": { ... } } } 형태로 전송
                java.util.Map<String, Object> inputData = new java.util.HashMap<>();
                inputData.put("action", "create_magazine");
                inputData.put("data", data);

                // 응답은 { "status": "COMPLETED", "output": { ... } } 형태
                responseBody = runPodService.sendRequest(pythonApiUrl, inputData);
            }

            // 4. 결과 파싱 (RunPod는 output 안에, 로컬은 body 자체가 결과일 수 있음)
            Object outputData = responseBody;
            if (responseBody.containsKey("output")) {
                outputData = responseBody.get("output");
            }

            // output을 MagazineCreateRequest로 변환
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            MagazineCreateRequest generatedData;
            try {
                generatedData = mapper.convertValue(outputData, MagazineCreateRequest.class);
            } catch (Exception e) {
                log.error("Failed to parse AI server response to MagazineCreateRequest. Data: {}", outputData, e);
                throw new RuntimeException("AI 서버 응답 형식이 올바르지 않습니다: " + e.getMessage());
            }

            if (generatedData == null) {
                log.error("Generated data is null after conversion. Output was: {}", outputData);
                throw new RuntimeException("AI 서버의 응답을 변환할 수 없습니다.");
            }

            if (generatedData.getSections() == null || generatedData.getSections().isEmpty()) {
                throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MAGAZINE + " (생성된 섹션이 없습니다)");
            }

            // [FIX] 문단(Paragraph)이 하나도 없는 빈 섹션만 있는지 딥 체크
            boolean hasValidParagraph = false;
            for (MagazineCreateRequest.SectionDto sectionDto : generatedData.getSections()) {
                if (sectionDto.getParagraphs() != null && !sectionDto.getParagraphs().isEmpty()) {
                    hasValidParagraph = true;
                    break;
                }
            }
            if (!hasValidParagraph) {
                throw new RuntimeException(ErrorMessages.FAILED_TO_GENERATE_MAGAZINE + " (생성된 유효한 문단이 하나도 없습니다)");
            }

            // 3. 받은 데이터로 저장 로직 수행
            Long magazineId = saveMagazine(generatedData, username);

            // 3-1. paragraph 없는 섹션 및 섹션 없는 매거진 자동 정리
            try {
                sectionService.cleanupEmptySectionsAndMagazines(magazineId);
            } catch (Exception e) {
                log.warn("Cleanup after magazine save failed: {}", e.getMessage());
            }

        // 무드보드이 = 커버이미지 정책 — 매거진 생성 후 비동기로 덮어씀
        try {
            log.info("Triggering async moodboard generation for magazine: {}", magazineId);
            moodboardService.createMoodboardForMagazineAsync(magazineId, username);
        } catch (Exception e) {
            log.error("Failed to trigger async moodboard generation for magazine {}: {}", magazineId, e.getMessage());
        }

            return magazineId;
        } catch (Exception e) {
            log.error("Error in generateAndSaveMagazine", e);
            throw new RuntimeException("Detailed error: " + e.getMessage(), e);
        }
    }

    // ⭐ Phase 1: 매거진 삭제
    @Transactional
    public void deleteMagazine(Long magazineId, String username) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        if (!magazine.isOwnedBy(user)) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }

        // FK 제약조건 해소: 모든 섹션의 열람 기록 먼저 삭제
        for (MagazineSection section : magazine.getSections()) {
            sectionService.deleteSectionViewHistory(section);
        }

        // 삭제 (CASCADE로 섹션, 상호작용도 자동 삭제)
        magazineRepository.delete(magazine);

        log.info("Magazine deleted successfully: magazineId={}, username={}", magazineId, username);
    }

    // ⭐ Phase 1: 매거진 수정
    @Transactional
    public void updateMagazine(Long magazineId, com.mine.api.dto.MagazineDto.UpdateRequest request, String username) {
        if (!request.isValid()) {
            throw new IllegalArgumentException("제목 또는 소개 중 최소 하나는 입력해야 합니다");
        }

        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        if (!magazine.isOwnedBy(user)) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }

        magazine.updateInfo(request.getTitle()); // 6. 저장 (변경 감지로 자동 저장)
        magazineRepository.save(magazine);

        log.info("Magazine updated successfully: magazineId={}, username={}", magazineId, username);
    }

    /**
     * 좋아요 토글 (좋아요 <-> 좋아요 취소)
     */
    @Transactional
    public boolean toggleLike(Long magazineId, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        java.util.Optional<com.mine.api.domain.MagazineLike> like = magazineLikeRepository.findByUserAndMagazine(user,
                magazine);

        if (like.isPresent()) {
            magazineLikeRepository.delete(like.get());
            log.info("Like removed for magazine {} by user {}", magazineId, username);
            return false; // 좋아요 취소됨
        } else {
            try {
                magazineLikeRepository.save(com.mine.api.domain.MagazineLike.builder()
                        .user(user)
                        .magazine(magazine)
                        .build());
                log.info("Like added for magazine {} by user {}", magazineId, username);
                return true; // 좋아요 추가됨
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 동시성 문제로 이미 저장된 경우, 좋아요가 있는 것으로 간주
                log.warn("Like already exists for magazine {} by user {} (concurrency issue)", magazineId, username);
                return true;
            }
        }
    }

    /**
     * 내가 좋아요한 매거진 목록 조회
     */
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getLikedMagazines(
            String username, org.springframework.data.domain.Pageable pageable) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        return magazineLikeRepository.findLikedMagazinesByUser(user, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // ⭐ 공개된 매거진 전체 조회 (인증 불필요)
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getPublicMagazines(
            Long userId,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.domain.Page<Magazine> magazines;

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

            // 유저가 비공개면 빈 페이지 반환 (또는 에러 처리)
            if (!user.getIsPublic()) {
                throw new SecurityException(ErrorMessages.PRIVATE_ACCOUNT);
            }

            magazines = magazineRepository.findAllByUserId(user.getId(), pageable);

            // 이미 유저 정보를 알고 있으므로, DTO 변환 시 이를 재사용 (Lazy Loading 방지)
            return magazines.map(m -> com.mine.api.dto.MagazineDto.ListItem.builder()
                    .id(m.getId())
                    .title(m.getTitle())
                    .coverImageUrl(m.getCoverImageUrl())
                    .username(user.getUsername()) // 조회한 유저 이름 사용
                    .likeCount((int) magazineLikeRepository.countByMagazine(m))
                    .commentCount(0)
                    .createdAt(m.getCreatedAt().toString())
                    .build());
        } else {
            // 전체 조회 시에는 EntityGraph가 적용된 쿼리를 사용하므로 기존 방식 유지
            magazines = magazineRepository.findByUserIsPublicTrue(pageable);
            return magazines.map(com.mine.api.dto.MagazineDto.ListItem::from);
        }
    }

    // ⭐ Phase 2: 키워드 검색 (보관)
    // public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> searchByKeyword(
    //         String keyword, String username, org.springframework.data.domain.Pageable pageable) {
    //     
    //     // Native Query에서 Order By 절 지원을 위해 Entity 필드(createdAt)를 DB 컬럼명(created_at)으로 변환
    //     org.springframework.data.domain.Pageable nativePageable = org.springframework.data.domain.PageRequest.of(
    //             pageable.getPageNumber(), pageable.getPageSize(), 
    //             org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"));
    // 
    //     return magazineRepository.searchByKeyword(keyword, username, nativePageable)
    //             .map(com.mine.api.dto.MagazineDto.ListItem::from);
    // }

    /**
     * 찜한 매거진 내에서 키워드 검색 (저장한 매거진용)
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> searchLikedMagazines(
            String keyword, String username, org.springframework.data.domain.Pageable pageable) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        org.springframework.data.domain.Pageable nativePageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"));

        return magazineRepository.searchLikedMagazines(keyword, user.getId(), nativePageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    /**
     * 둘러보기(추천 피드) 대상 키워드 검색 (공개 매거진 대상)
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> searchExploreMagazines(
            String keyword, String username, org.springframework.data.domain.Pageable pageable) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        org.springframework.data.domain.Pageable nativePageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"));

        // 개인화 필터링을 위한 키워드 추출
        java.util.List<String> top3Keywords = getTop3InterestKeywords(user);
        String kw1 = top3Keywords.size() > 0 ? top3Keywords.get(0) : "";
        String kw2 = top3Keywords.size() > 1 ? top3Keywords.get(1) : "";
        String kw3 = top3Keywords.size() > 2 ? top3Keywords.get(2) : "";

        return magazineRepository.searchPersonalizedExploreMagazines(keyword, kw1, kw2, kw3, user.getId(), nativePageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    /**
     * 사용자의 관심 키워드 상위 3개를 추출하는 공통 메서드
     */
    private java.util.List<String> getTop3InterestKeywords(User user) {
        // 1. 관심사 키워드
        java.util.List<String> interestKeywords = userInterestRepository.findByUser(user).stream()
                .map(ui -> ui.getInterest().getName())
                .collect(java.util.stream.Collectors.toList());

        // 2. 좋아요한 매거진의 태그
        java.util.List<Magazine> likedMagazines = magazineLikeRepository.findAllLikedMagazinesByUser(user);
        java.util.Set<String> likedTags = new java.util.HashSet<>();
        for (Magazine m : likedMagazines) {
            if (m.getTags() != null && !m.getTags().isEmpty()) {
                String[] tags = m.getTags().split(",");
                for (String tag : tags) {
                    likedTags.add(tag.trim());
                }
            }
        }

        // 3. 중복 제거 및 정렬
        java.util.List<String> allKeywords = new java.util.ArrayList<>();
        allKeywords.addAll(interestKeywords);
        allKeywords.addAll(likedTags);

        java.util.List<String> uniqueKeywords = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(allKeywords));
        java.util.Collections.sort(uniqueKeywords);

        // 상위 3개만 반환
        return uniqueKeywords.stream().limit(3).collect(java.util.stream.Collectors.toList());
    }

    // ⭐ 공개 계정의 매거진 조회 (인증 불필요) - 사용자 공개 AND 매거진 공개
    public Magazine getPublicMagazine(Long magazineId) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        // 계정이 비공개면 접근 불가 (매거진 개별 공개 여부는 없음)
        if (!magazine.getUser().getIsPublic()) {
            throw new SecurityException(ErrorMessages.PRIVATE_ACCOUNT);
        }

        // displayOrder 순으로 섹션 정렬
        magazine.getSections().sort((s1, s2) -> {
            Integer o1 = s1.getDisplayOrder() != null ? s1.getDisplayOrder() : Integer.MAX_VALUE;
            Integer o2 = s2.getDisplayOrder() != null ? s2.getDisplayOrder() : Integer.MAX_VALUE;
            return o1.compareTo(o2);
        });

        return magazine;
    }

    // ⭐ 커버 이미지 변경
    @org.springframework.transaction.annotation.Transactional
    public void updateCover(Long magazineId, String newCoverUrl, String username) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }

        magazine.setCoverImageUrl(newCoverUrl);
        magazineRepository.save(magazine);
    }

    // ⭐ Phase 2: 키워드 검색 (보관)
    // public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> searchByKeyword(
    //         String keyword, String username, org.springframework.data.domain.Pageable pageable) {
    //     
    //     // Native Query에서 Order By 절 지원을 위해 Entity 필드(createdAt)를 DB 컬럼명(created_at)으로 변환
    //     org.springframework.data.domain.Pageable nativePageable = org.springframework.data.domain.PageRequest.of(
    //             pageable.getPageNumber(), pageable.getPageSize(), 
    //             org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"));
    // 
    //     return magazineRepository.searchByKeyword(keyword, username, nativePageable)
    //             .map(com.mine.api.dto.MagazineDto.ListItem::from);
    // }

    // ⭐ Phase 2: 내 매거진 조회
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getMyMagazinesPage(
            String username, org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.findByUserUsername(username, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // ⭐ Phase 4: 개인화 피드 (커서 기반) - 좋아요 + 관심사 기반
    public com.mine.api.dto.CursorResponse<com.mine.api.dto.MagazineDto.ListItem> getPersonalizedFeedCursor(
            String username, Long cursorId, int limit, boolean isTest) {
        
        // 테스트 모드인 경우 전체 피드 반환
        if (isTest) {
            return getTestFeedCursor(cursorId, limit);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        // 3. 키워드 조합 (관심사 + 좋아요 태그에서 상위 키워드 추출)
        java.util.List<String> top3Keywords = getTop3InterestKeywords(user);
        String keyword1 = top3Keywords.size() > 0 ? top3Keywords.get(0) : "";
        String keyword2 = top3Keywords.size() > 1 ? top3Keywords.get(1) : "";
        String keyword3 = top3Keywords.size() > 2 ? top3Keywords.get(2) : "";

        // 4. 쿼리 실행 (limit + 1개 조회)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1);
        java.util.List<Magazine> magazines = magazineRepository.findRecommendedFeedCursor(
                keyword1, keyword2, keyword3, user.getId(), cursorId, pageable);

        boolean hasNext = false;
        if (magazines.size() > limit) {
            hasNext = true;
            magazines.remove(limit);
        }

        Long nextCursor = null;
        if (!magazines.isEmpty()) {
            nextCursor = magazines.get(magazines.size() - 1).getId();
        }

        java.util.List<com.mine.api.dto.MagazineDto.ListItem> content = magazines.stream()
                .map(com.mine.api.dto.MagazineDto.ListItem::from)
                .collect(java.util.stream.Collectors.toList());

    return new com.mine.api.dto.CursorResponse<>(content, nextCursor, hasNext);
    }

    /**
     * 프론트엔드 무한 스크롤 테스트용 피드 (모든 공개 매거진 최신순)
     */
    public com.mine.api.dto.CursorResponse<com.mine.api.dto.MagazineDto.ListItem> getTestFeedCursor(Long cursorId, int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit + 1);
        
        java.util.List<Magazine> magazines = magazineRepository.findPublicMagazinesCursor(cursorId, pageable);

        boolean hasNext = false;
        if (magazines.size() > limit) {
            hasNext = true;
            magazines.remove(limit);
        }

        Long nextCursor = null;
        if (!magazines.isEmpty()) {
            nextCursor = magazines.get(magazines.size() - 1).getId();
        }

        java.util.List<com.mine.api.dto.MagazineDto.ListItem> content = magazines.stream()
                .map(com.mine.api.dto.MagazineDto.ListItem::from)
                .collect(java.util.stream.Collectors.toList());

        return new com.mine.api.dto.CursorResponse<>(content, nextCursor, hasNext);
    }

    /**
     * 회원가입 직후 관심사 기반 매거진 자동 생성 (비동기)
     * Listener에서 호출될 때 프록시를 통해 별도 스레드에서 실행됨
     */
    @org.springframework.scheduling.annotation.Async
    @Transactional
    public void generateInitialMagazinesAsync(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            java.util.List<String> interests = userInterestRepository.findByUser(user).stream()
                    .map(ui -> ui.getInterest().getCode())
                    .collect(java.util.stream.Collectors.toList());

            if (interests == null || interests.isEmpty()) {
                log.warn("No interests found for user: {}. Skipping initial magazine generation.", username);
                return;
            }

            // 1. 관심사 목록 랜덤 추출
            java.util.List<String> targetInterests = new java.util.ArrayList<>(interests);
            java.util.Collections.shuffle(targetInterests);
            targetInterests = targetInterests.subList(0, Math.min(2, targetInterests.size()));

            log.info("Starting initial magazine generation for user: {} with interests: {}", username, targetInterests);

            for (String interestCode : targetInterests) {
                try {
                    com.mine.api.dto.MagazineGenerationRequest genRequest = new com.mine.api.dto.MagazineGenerationRequest();
                    genRequest.setTopic(interestCode);
                    genRequest.setUserMood("vibrant");

                    log.info("Generating welcome magazine for interest: {} (User: {})", interestCode, username);
                    this.generateAndSaveMagazine(genRequest, username);
                    
                    Thread.sleep(5000); 
                    
                } catch (Exception e) {
                    log.error("Failed to generate initial magazine for user: {}", username, e);
                }
            }
        } catch (Exception e) {
            log.error("Error in generateInitialMagazinesAsync", e);
        }
    }

    // [REFACTORED] 이벤트 리스너 로직은 MagazineGenerationListener로 이동되었습니다.
}
