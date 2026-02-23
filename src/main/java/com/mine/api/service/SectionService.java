package com.mine.api.service;

import com.mine.api.common.ErrorMessages;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.User;
import com.mine.api.dto.ParagraphDto;
import com.mine.api.dto.SectionDto;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.MagazineSectionRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SectionService {

    private final MagazineRepository magazineRepository;
    private final MagazineSectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final RunPodService runPodService;
    private final SectionViewHistoryService sectionViewHistoryService;
    private final S3Service s3Service;

    @Value("${python.api.url}")
    private String pythonApiUrl;

    /**
     * 섹션 상세 조회 (열람 기록 저장 포함)
     */
    @Transactional
    public SectionDto.Response getSection(Long magazineId, Long sectionId, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);
        MagazineSection section = getSectionFromMagazine(magazine, sectionId);

        // 열람 기록 저장
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));
        sectionViewHistoryService.recordView(user, section);

        return toResponse(section);
    }

    /**
     * 섹션 삭제 (프롬프트 없이 직접 삭제)
     */
    @Transactional
    public void deleteSection(Long magazineId, Long sectionId, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);
        MagazineSection section = getSectionFromMagazine(magazine, sectionId);

        magazine.getSections().remove(section);
        sectionRepository.delete(section);

        // 순서 재정렬
        reorderAfterDelete(magazine);

        log.info("Section deleted: magazineId={}, sectionId={}, username={}",
                magazineId, sectionId, username);
    }

    /**
     * 섹션 직접 수정 (AI 없이)
     */
    @Transactional
    public SectionDto.Response updateSection(Long magazineId, Long sectionId,
            SectionDto.UpdateRequest request, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);
        MagazineSection section = getSectionFromMagazine(magazine, sectionId);

        // 요청에 포함된 필드만 업데이트
        if (request.getHeading() != null) {
            section.setHeading(request.getHeading());
        }

        sectionRepository.save(section);
        log.info("Section updated: magazineId={}, sectionId={}, username={}",
                magazineId, sectionId, username);

        return toResponse(section);
    }

    /**
     * 섹션 순서 변경 (드래그 앤 드롭)
     */
    @Transactional
    public void reorderSections(Long magazineId, List<Long> sectionIds, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);

        // 순서대로 displayOrder 업데이트
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sectionId = sectionIds.get(i);
            MagazineSection section = getSectionFromMagazine(magazine, sectionId);
            section.setDisplayOrder(i);
        }

        magazineRepository.save(magazine);
        log.info("Sections reordered: magazineId={}, newOrder={}", magazineId, sectionIds);
    }

    /**
     * 섹션 레벨 상호작용 (AI 프롬프트로 본문 수정)
     */
    @Transactional
    public SectionDto.InteractResponse interact(Long magazineId, Long sectionId,
            String message, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);
        MagazineSection section = getSectionFromMagazine(magazine, sectionId);

        Map<String, Object> data = new HashMap<>();
        data.put("action", "edit_section");
        data.put("magazine_id", magazineId);
        data.put("section_id", sectionId);
        data.put("section_data", convertSectionToMap(section));
        data.put("message", message);

        log.info("Sending edit_section request: magazineId={}, sectionId={}, message={}",
                magazineId, sectionId, message);

        Map<String, Object> responseBody;
        Map<String, Object> output;

        // 로컬 환경 vs RunPod 환경 분기
        if (pythonApiUrl.contains("localhost") || pythonApiUrl.contains("127.0.0.1")) {
            // 로컬: sendSyncRequest 사용 (플랫 JSON)
            responseBody = runPodService.sendSyncRequest(pythonApiUrl, data);
            output = responseBody; // 로컬은 output 래핑 없음
        } else {
            // RunPod: sendRequest 사용 (input 래핑 + 비동기 폴링)
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("action", "edit_section");
            inputData.put("data", data);
            responseBody = runPodService.sendRequest(pythonApiUrl, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputTemp = (Map<String, Object>) responseBody.get("output");
            output = outputTemp;
        }

        if (output == null) {
            throw new RuntimeException("Failed to get response from AI server");
        }

        // [DEBUG] Python 응답 전체 로깅
        log.info("[DEBUG] Full Python response: {}", output);

        String actionType = (String) output.get("intent");
        log.info("[DEBUG] Parsed intent: {}", actionType);

        @SuppressWarnings("unchecked")
        Map<String, Object> updatedSection = (Map<String, Object>) output.get("updated_section");
        log.info("[DEBUG] updated_section: {}", updatedSection);

        // 섹션 업데이트
        if (updatedSection != null) {
            // [NEW] 이미지 S3 변환
            String imageUrl = (String) updatedSection.get("image_url");
            if (imageUrl != null) {
                updatedSection.put("image_url", s3Service.uploadImageFromUrl(imageUrl));
            }

            section.update(
                    (String) updatedSection.get("heading"),
                    (String) updatedSection.get("layout_hint"),
                    (String) updatedSection.get("layout_type"));
            sectionRepository.save(section);
        }

        String aiMessage = "섹션이 업데이트되었습니다.";
        if (updatedSection != null && updatedSection.get("heading") != null) {
            aiMessage = (String) updatedSection.get("heading");
        }

        return SectionDto.InteractResponse.builder()
                .message(aiMessage)
                .actionType(actionType)
                .sectionId(sectionId)
                .section(toResponse(section))
                .build();
    }

    // ===== Helper Methods =====

    private Magazine getMagazineWithOwnerCheck(Long magazineId, String username) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.MAGAZINE_NOT_FOUND));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }
        return magazine;
    }

    private MagazineSection getSectionFromMagazine(Magazine magazine, Long sectionId) {
        return magazine.getSections().stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
    }

    private void reorderAfterDelete(Magazine magazine) {
        List<MagazineSection> sections = magazine.getSections();
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).setDisplayOrder(i);
        }
    }

    private Map<String, Object> convertSectionToMap(MagazineSection section) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", section.getId());
        map.put("heading", section.getHeading());
        map.put("thumbnail_url", section.getThumbnailUrl());

        // paragraphs 변환
        List<Map<String, Object>> paragraphsList = section.getParagraphs().stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("subtitle", p.getSubtitle());
                    pMap.put("text", p.getText());
                    pMap.put("image_url", p.getImageUrl());
                    return pMap;
                })
                .collect(Collectors.toList());
        map.put("paragraphs", paragraphsList);

        map.put("layout_hint", section.getLayoutHint());
        map.put("layout_type", section.getLayoutType());
        return map;
    }

    private SectionDto.Response toResponse(MagazineSection section) {
        // paragraphs 변환
        List<ParagraphDto.Response> paragraphsList = section.getParagraphs().stream()
                .map(p -> ParagraphDto.Response.builder()
                        .id(p.getId())
                        .subtitle(p.getSubtitle())
                        .text(p.getText())
                        .imageUrl(p.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return SectionDto.Response.builder()
                .id(section.getId())
                .heading(section.getHeading())
                .thumbnailUrl(section.getThumbnailUrl())
                .paragraphs(paragraphsList)
                .layoutType(section.getLayoutType())
                .layoutHint(section.getLayoutHint())
                .displayOrder(section.getDisplayOrder())
                .build();
    }
}
