package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final com.mine.api.repository.MagazineLikeRepository magazineLikeRepository;
    private final com.mine.api.repository.UserInterestRepository userInterestRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${python.api.url}")
    private String pythonApiUrl;

    @Transactional
    public Long saveMagazine(MagazineCreateRequest request, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Magazine 엔티티 생성
        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .introduction(request.getIntroduction())
                .coverImageUrl(request.getCoverImageUrl())
                .user(user)
                .build();

        // 2. Section 엔티티 생성 및 연관관계 설정
        if (request.getSections() != null) {
            for (MagazineCreateRequest.SectionDto sectionDto : request.getSections()) {
                MagazineSection section = MagazineSection.builder()
                        .heading(sectionDto.getHeading())
                        .content(sectionDto.getContent())
                        .imageUrl(sectionDto.getImageUrl())
                        .layoutHint(sectionDto.getLayoutHint())
                        .build();
                magazine.addSection(section);
            }
        }

        // 3. 저장 (CascadeType.ALL로 인해 Section도 함께 저장됨)
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

        return magazine;
    }

    @Transactional
    public Long generateAndSaveMagazine(com.mine.api.dto.MagazineGenerationRequest request, String username) {
        // 1. 사용자 관심사 조회
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        java.util.List<String> userInterests = userInterestRepository.findByUser(user).stream()
                .map(ui -> ui.getInterest().name())
                .collect(java.util.stream.Collectors.toList());

        // 2. Python 서버로 요청 (관심사 포함)
        java.util.Map<String, Object> pythonRequest = new java.util.HashMap<>();
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_TOPIC, request.getTopic());
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_USER_MOOD, request.getUserMood());
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_USER_EMAIL, username);
        pythonRequest.put("user_interests", userInterests); // 관심사 추가

        MagazineCreateRequest generatedData = restTemplate.postForObject(pythonApiUrl, pythonRequest,
                MagazineCreateRequest.class);

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
            magazineLikeRepository.save(com.mine.api.domain.MagazineLike.builder()
                    .user(user)
                    .magazine(magazine)
                    .build());
            return true; // 좋아요 추가됨
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

        return magazine;
    }

    // ⭐ Phase 2: 키워드 검색
    public org.springframework.data.domain.Page<Magazine> searchByKeyword(
            String keyword, org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.searchByKeyword(keyword, pageable);
    }

    // ⭐ Phase 2: 태그 검색 (Magazine에 tags 필드 추가 후 사용)
    // public org.springframework.data.domain.Page<Magazine> searchByTags(
    // java.util.List<String> tags, org.springframework.data.domain.Pageable
    // pageable) {
    // return magazineRepository.findByTagsIn(tags, pageable);
    // }

    // ⭐ Phase 2: 내 매거진 조회
    public org.springframework.data.domain.Page<Magazine> getMyMagazinesPage(
            String username, org.springframework.data.domain.Pageable pageable) {
        return magazineRepository.findByUserUsername(username, pageable);
    }
}
