package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
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

    @Value("${python.api.url}")
    private String pythonApiUrl;

    /**
     * 섹션 상세 조회
     */
    public SectionDto.Response getSection(Long magazineId, Long sectionId, String username) {
        Magazine magazine = getMagazineWithOwnerCheck(magazineId, username);
        MagazineSection section = getSectionFromMagazine(magazine, sectionId);
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

        String actionType = (String) output.get("intent");

        @SuppressWarnings("unchecked")
        Map<String, Object> updatedSection = (Map<String, Object>) output.get("updated_section");

        // 섹션 업데이트
        if (updatedSection != null) {
            section.update(
                    (String) updatedSection.get("heading"),
                    (String) updatedSection.get("content"),
                    (String) updatedSection.get("image_url"),
                    (String) updatedSection.get("layout_hint"),
                    (String) updatedSection.get("layout_type"),
                    (String) updatedSection.get("caption"));
            sectionRepository.save(section);
        }

        String aiMessage = updatedSection != null ? (String) updatedSection.get("heading") : "섹션이 업데이트되었습니다.";

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
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found: " + magazineId));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new SecurityException("You don't have permission to access this magazine");
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
        map.put("content", section.getContent());
        map.put("image_url", section.getImageUrl());
        map.put("layout_hint", section.getLayoutHint());
        map.put("layout_type", section.getLayoutType());
        map.put("caption", section.getCaption());
        return map;
    }

    private SectionDto.Response toResponse(MagazineSection section) {
        return SectionDto.Response.builder()
                .id(section.getId())
                .heading(section.getHeading())
                .content(section.getContent())
                .imageUrl(section.getImageUrl())
                .layoutType(section.getLayoutType())
                .layoutHint(section.getLayoutHint())
                .caption(section.getCaption())
                .displayOrder(section.getDisplayOrder())
                .build();
    }
}
