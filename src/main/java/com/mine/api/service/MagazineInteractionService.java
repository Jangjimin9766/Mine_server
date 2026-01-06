package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineInteraction;
import com.mine.api.domain.MagazineSection;
import com.mine.api.dto.InteractionDto;
import com.mine.api.repository.MagazineInteractionRepository;
import com.mine.api.repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
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
@lombok.extern.slf4j.Slf4j
public class MagazineInteractionService {

    private final MagazineRepository magazineRepository;
    private final MagazineInteractionRepository interactionRepository;
    private final RunPodService runPodService;

    @Value("${python.api.url}")
    private String pythonApiUrl;

    @Transactional
    public InteractionDto.InteractResponse interact(Long magazineId, String username,
            InteractionDto.InteractRequest request) {
        // 1. 매거진 조회 및 권한 확인
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You don't have permission to modify this magazine");
        }

        // 2. RunPod Serverless로 요청 (edit_magazine action)
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("action", "edit_magazine");

        Map<String, Object> data = new HashMap<>();
        data.put("magazine_id", magazineId);
        data.put("magazine_data", convertMagazineToMap(magazine));
        data.put("message", request.getMessage());
        inputData.put("data", data);

        log.info("Sending edit_magazine request to RunPod: magazineId={}, message={}", magazineId,
                request.getMessage());

        // RunPod 요청 → 폴링 → 응답
        Map<String, Object> runPodResponse = runPodService.sendRequest(pythonApiUrl, inputData);

        // output 필드에서 실제 결과 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> pythonResponse = (Map<String, Object>) runPodResponse.get("output");

        if (pythonResponse == null) {
            throw new RuntimeException("Failed to get response from AI server: no output");
        }

        log.info("RunPod response received: {}", pythonResponse);

        String aiMessage = (String) pythonResponse.get("message");
        String actionType = (String) pythonResponse.get("action");

        // 3. 응답에 따라 매거진 업데이트
        handlePythonResponse(magazine, pythonResponse);

        // 4. 상호작용 이력 저장
        MagazineInteraction interaction = MagazineInteraction.builder()
                .magazine(magazine)
                .userMessage(request.getMessage())
                .aiResponse(aiMessage)
                .actionType(actionType)
                .build();
        interactionRepository.save(interaction);

        // 5. 응답 반환
        InteractionDto.InteractResponse response = new InteractionDto.InteractResponse();
        response.setMessage(aiMessage);
        response.setActionType(actionType);
        response.setMagazineId(magazineId);
        return response;
    }

    public List<InteractionDto.InteractionHistory> getInteractionHistory(Long magazineId, String username) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You don't have permission to view this magazine");
        }

        return interactionRepository.findByMagazineOrderByCreatedAtDesc(magazine).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertMagazineToMap(Magazine magazine) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", magazine.getId());
        map.put("title", magazine.getTitle());
        map.put("introduction", magazine.getIntroduction());
        map.put("cover_image_url", magazine.getCoverImageUrl());
        map.put("sections", convertSectionsToMap(magazine.getSections()));
        return map;
    }

    private List<Map<String, Object>> convertSectionsToMap(List<MagazineSection> sections) {
        return sections.stream()
                .map(section -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("heading", section.getHeading());
                    map.put("content", section.getContent());
                    map.put("image_url", section.getImageUrl());
                    map.put("layout_hint", section.getLayoutHint());
                    map.put("layout_type", section.getLayoutType());
                    map.put("caption", section.getCaption());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    protected void handlePythonResponse(Magazine magazine, Map<String, Object> response) {
        String action = (String) response.get("action");

        // 1. 섹션 재생성
        if ("regenerate_section".equals(action)) {
            Integer sectionIndex = (Integer) response.get("section_index");
            @SuppressWarnings("unchecked")
            Map<String, Object> newSection = (Map<String, Object>) response.get("new_section");

            if (sectionIndex != null && newSection != null && sectionIndex >= 0
                    && sectionIndex < magazine.getSections().size()) {
                MagazineSection section = magazine.getSections().get(sectionIndex);
                section.update(
                        (String) newSection.get("heading"),
                        (String) newSection.get("content"),
                        (String) newSection.get("image_url"),
                        (String) newSection.get("layout_hint"),
                        (String) newSection.get("layout_type"),
                        (String) newSection.get("caption"));
            }
        }

        // 2. 섹션 추가
        else if ("add_section".equals(action)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> newSection = (Map<String, Object>) response.get("new_section");

            if (newSection != null) {
                MagazineSection section = MagazineSection.builder()
                        .heading((String) newSection.get("heading"))
                        .content((String) newSection.get("content"))
                        .imageUrl((String) newSection.get("image_url"))
                        .layoutHint((String) newSection.get("layout_hint"))
                        .layoutType((String) newSection.get("layout_type"))
                        .caption((String) newSection.get("caption"))
                        .build();
                section.setMagazine(magazine);
                magazine.getSections().add(section);
            }
        }

        // 3. 섹션 삭제
        else if ("delete_section".equals(action)) {
            Integer sectionIndex = (Integer) response.get("section_index");

            if (sectionIndex != null && sectionIndex >= 0 && sectionIndex < magazine.getSections().size()) {
                magazine.getSections().remove(sectionIndex.intValue());
            }
        }

        // 4. 전체 톤 변경 (모든 섹션 교체)
        else if ("change_tone".equals(action)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> newSections = (List<Map<String, Object>>) response.get("new_sections");

            if (newSections != null) {
                magazine.getSections().clear();

                for (Map<String, Object> sec : newSections) {
                    MagazineSection section = MagazineSection.builder()
                            .heading((String) sec.get("heading"))
                            .content((String) sec.get("content"))
                            .imageUrl((String) sec.get("image_url")) // Python snake_case
                            .layoutHint((String) sec.get("layout_hint")) // Python snake_case
                            .layoutType((String) sec.get("layout_type"))
                            .caption((String) sec.get("caption"))
                            .build();
                    section.setMagazine(magazine);
                    magazine.getSections().add(section);
                }
            }
        }

        // ⭐ 변경사항 DB에 저장
        magazineRepository.save(magazine);
    }

    private InteractionDto.InteractionHistory convertToHistoryDto(MagazineInteraction interaction) {
        InteractionDto.InteractionHistory dto = new InteractionDto.InteractionHistory();
        dto.setId(interaction.getId());
        dto.setUserMessage(interaction.getUserMessage());
        dto.setAiResponse(interaction.getAiResponse());
        dto.setActionType(interaction.getActionType());
        dto.setCreatedAt(interaction.getCreatedAt().toString());
        return dto;
    }
}
