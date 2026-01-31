package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineLike;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.User;
import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.UserRepository;
import com.mine.api.repository.MagazineLikeRepository;
import com.mine.api.repository.UserInterestRepository;
import com.mine.api.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final UserRepository userRepository;
    private final MagazineLikeRepository magazineLikeRepository;
    private final UserInterestRepository userInterestRepository;
    private final FollowRepository followRepository;
    private final RunPodService runPodService;
    private final MoodboardService moodboardService;

    @org.springframework.beans.factory.annotation.Value("${python.api.url}")
    private String pythonApiUrl;

    @org.springframework.beans.factory.annotation.Value("${python.api.key}")
    private String pythonApiKey;

    @Transactional
    public Long saveMagazine(MagazineCreateRequest request, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. 태그 리스트를 JSON 문자열로 변환
        String tagsJson = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagsJson = String.join(",", request.getTags());
        }

        // 2. 무드보드 정보 추출
        String moodboardImageUrl = null;
        String moodboardDescription = null;
        if (request.getMoodboard() != null) {
            moodboardImageUrl = request.getMoodboard().getImage_url();
            moodboardDescription = request.getMoodboard().getDescription();

            // Base64 이미지인 경우 S3 업로드 처리
            if (moodboardImageUrl != null
                    && (moodboardImageUrl.startsWith("data:image") || moodboardImageUrl.length() > 255)) {
                moodboardImageUrl = moodboardService.uploadBase64ToS3(moodboardImageUrl);
            }
        }

        // 3. Magazine 엔티티 생성
        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .introduction(request.getIntroduction())
                .coverImageUrl(request.getCoverImageUrl())
                .tags(tagsJson)
                .moodboardImageUrl(moodboardImageUrl)
                .moodboardDescription(moodboardDescription)
                .user(user)
                .build();

        // 4. Section 엔티티 생성 및 연관관계 설정
        if (request.getSections() != null) {
            for (int i = 0; i < request.getSections().size(); i++) {
                MagazineCreateRequest.SectionDto sectionDto = request.getSections().get(i);
                MagazineSection section = MagazineSection.builder()
                        .heading(sectionDto.getHeading())
                        .content(sectionDto.getContent())
                        .imageUrl(sectionDto.getImageUrl())
                        .layoutHint(sectionDto.getLayoutHint())
                        .layoutType(sectionDto.getLayoutType())
                        .caption(sectionDto.getCaption())
                        .displayOrder(i) // 생성 순서대로 0부터 할당
                        .build();
                magazine.addSection(section);
            }
        }

        // 5. 저장 (CascadeType.ALL로 인해 Section도 함께 저장됨)
        Magazine savedMagazine = magazineRepository.save(magazine);
        return savedMagazine.getId();
    }

    /**
     * N+1 쿼리 방지: fetch join으로 sections와 user를 한 번에 조회
     */
    public java.util.List<Magazine> getMagazinesByUser(String username) {
        return magazineRepository.findByUserUsernameWithSections(username);
    }

    public Magazine getMagazineDetail(Long id, String username) {
        Magazine magazine = magazineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized access to this magazine");
        }

        // displayOrder 순으로 섹션 정렬
        magazine.getSections().sort((s1, s2) -> {
            Integer o1 = s1.getDisplayOrder() != null ? s1.getDisplayOrder() : Integer.MAX_VALUE;
            Integer o2 = s2.getDisplayOrder() != null ? s2.getDisplayOrder() : Integer.MAX_VALUE;
            return o1.compareTo(o2);
        });

        return magazine;
    }

    @Transactional
    public Long generateAndSaveMagazine(com.mine.api.dto.MagazineGenerationRequest request, String username) {
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
            // { "topic": ..., "user_mood": ..., "user_email": ... } 형태로 전송
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
        MagazineCreateRequest generatedData = mapper.convertValue(outputData, MagazineCreateRequest.class);

        if (generatedData == null) {
            throw new RuntimeException("Failed to generate magazine from AI server");
        }

        // 3. 받은 데이터로 저장 로직 수행
        return saveMagazine(generatedData, username);
    }

    // ⭐ Phase 1: 매거진 삭제
    @Transactional
    public void deleteMagazine(Long magazineId, String username) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("매거진을 찾을 수 없습니다: " + magazineId));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        if (!magazine.isOwnedBy(user)) {
            throw new SecurityException("삭제 권한이 없습니다");
        }
        // 4. 삭제 (CASCADE로 섹션, 상호작용도 자동 삭제)
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
                .orElseThrow(() -> new IllegalArgumentException("매거진을 찾을 수 없습니다: " + magazineId));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        if (!magazine.isOwnedBy(user)) {
            throw new SecurityException("수정 권한이 없습니다");
        }

        magazine.updateInfo(request.getTitle(), request.getIntroduction()); // 6. 저장 (변경 감지로 자동 저장)
        magazineRepository.save(magazine);

        log.info("Magazine updated successfully: magazineId={}, username={}", magazineId, username);
    }

    // ⭐ Phase 2: 공개/비공개 설정
    @Transactional
    public com.mine.api.dto.MagazineDto.VisibilityResponse setVisibility(
            Long magazineId, Boolean isPublic, String username) {

        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("매거진을 찾을 수 없습니다"));

        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!magazine.isOwnedBy(user)) {
            throw new SecurityException("권한이 없습니다");
        }

        String shareToken = magazine.setPublic(isPublic);
        magazineRepository.save(magazine);

        String shareUrl = null;
        if (isPublic && shareToken != null) {
            shareUrl = "http://localhost:3000/share/" + shareToken;
        }

        log.info("Magazine visibility updated: magazineId={}, isPublic={}, shareToken={}",
                magazineId, isPublic, shareToken);

        return new com.mine.api.dto.MagazineDto.VisibilityResponse(isPublic, shareUrl);
    }

    /**
     * 좋아요 토글 (좋아요 <-> 좋아요 취소)
     */
    @Transactional
    public boolean toggleLike(Long magazineId, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        java.util.Optional<com.mine.api.domain.MagazineLike> like = magazineLikeRepository.findByUserAndMagazine(user,
                magazine);

        if (like.isPresent()) {
            magazineLikeRepository.delete(like.get());
            return false; // 좋아요 취소됨
        } else {
            try {
                magazineLikeRepository.save(com.mine.api.domain.MagazineLike.builder()
                        .user(user)
                        .magazine(magazine)
                        .build());
                return true; // 좋아요 추가됨
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 동시성 문제로 이미 저장된 경우, 좋아요가 있는 것으로 간주
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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return magazineLikeRepository.findLikedMagazinesByUser(user, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // ⭐ Phase 2: 공유 토큰으로 조회 (인증 불필요)
    public Magazine getByShareToken(String shareToken) {
        Magazine magazine = magazineRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 공유 링크입니다"));

        if (!magazine.getIsPublic()) {
            throw new SecurityException("비공개 매거진입니다");
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
                .orElseThrow(() -> new IllegalArgumentException("매거진을 찾을 수 없습니다: " + magazineId));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new SecurityException("권한이 없습니다");
        }

        magazine.setCoverImageUrl(newCoverUrl);
        magazineRepository.save(magazine);
    }

    // ⭐ Phase 2: 키워드 검색
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> searchByKeyword(
            String keyword, String username, org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.searchByKeyword(keyword, username, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // ⭐ Phase 2: 내 매거진 조회
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getMyMagazinesPage(
            String username, org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.findByUserUsername(username, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // ⭐ Phase 4: 개인화 피드
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getPersonalizedFeed(
            String username,
            org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 1. 팔로잉 목록 가져오기
        java.util.List<User> followings = followRepository.findByFollower(user).stream()
                .map(com.mine.api.domain.Follow::getFollowing)
                .collect(java.util.stream.Collectors.toList());

        // 2. 관심사 중 하나 랜덤 선택
        java.util.List<com.mine.api.domain.UserInterest> interests = userInterestRepository.findByUser(user);
        String keyword = "";
        if (!interests.isEmpty()) {
            int randomIndex = new java.util.Random().nextInt(interests.size());
            keyword = interests.get(randomIndex).getInterest().getName();
        }

        // 3. 쿼리 실행 (팔로잉 OR 관심사 키워드)
        return magazineRepository.findPersonalizedFeed(followings, keyword, pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // 피드 조회 (비로그인/전체)
    public org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> getMagazineFeed(
            org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable)
                .map(com.mine.api.dto.MagazineDto.ListItem::from);
    }

    // 공개 매거진 상세 조회 (ID로)
    public Magazine getPublicMagazine(Long id) {
        Magazine magazine = magazineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));
        
        if (!Boolean.TRUE.equals(magazine.getIsPublic())) {
             throw new SecurityException("비공개 매거진입니다");
        }
        return magazine;
    }
}
