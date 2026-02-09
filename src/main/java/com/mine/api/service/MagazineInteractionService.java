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
    private final S3Service s3Service;

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

        // 2. Python AI 서버로 요청 (edit_magazine action)
        Map<String, Object> data = new HashMap<>();
        data.put("action", "edit_magazine");
        data.put("magazine_id", magazineId);
        data.put("magazine_data", convertMagazineToMap(magazine));
        data.put("message", request.getMessage());

        log.info("Sending edit_magazine request: magazineId={}, message={}", magazineId,
                request.getMessage());

        Map<String, Object> responseBody;
        Map<String, Object> pythonResponse;

        // 로컬 환경 vs RunPod 환경 분기
        if (pythonApiUrl.contains("localhost") || pythonApiUrl.contains("127.0.0.1")) {
            // 로컬: sendSyncRequest 사용 (플랫 JSON)
            responseBody = runPodService.sendSyncRequest(pythonApiUrl, data);
            pythonResponse = responseBody; // 로컬은 output 래핑 없음
        } else {
            // RunPod: sendRequest 사용 (input 래핑 + 비동기 폴링)
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("action", "edit_magazine");
            inputData.put("data", data);
            responseBody = runPodService.sendRequest(pythonApiUrl, inputData);

            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException("Failed to get response from AI server: no output field");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> outputTemp = (Map<String, Object>) responseBody.get("output");
            pythonResponse = outputTemp;
        }

        if (pythonResponse == null) {
            throw new RuntimeException("Failed to get response from AI server: no output");
        }

        log.info("Python response received: {}", pythonResponse);

        // Python 응답 형식: {intent, success, updated_magazine: {heading, content, ...}}
        String actionType = (String) pythonResponse.get("intent"); // "intent" 필드 사용

        // updated_magazine에서 메시지 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedMagazine = (Map<String, Object>) pythonResponse.get("updated_magazine");
        String aiMessage = "매거진이 업데이트되었습니다.";
        if (updatedMagazine != null && updatedMagazine.get("heading") != null) {
            aiMessage = (String) updatedMagazine.get("heading");
        }

        // 3. 응답에 따라 매거진 업데이트 (updated_magazine 전달)
        handlePythonResponse(magazine, pythonResponse);

        // 4. 상호작용 이력 저장
        MagazineInteraction interaction = MagazineInteraction.builder()
                .magazine(magazine)
                .userMessage(request.getMessage())
                .aiResponse(aiMessage != null ? aiMessage : "업데이트 완료")
                .actionType(actionType != null ? actionType : "unknown")
                .build();
        interactionRepository.save(interaction);

        // 5. 응답 반환
        InteractionDto.InteractResponse response = new InteractionDto.InteractResponse();
        response.setMessage(aiMessage != null ? aiMessage : "매거진이 업데이트되었습니다.");
        response.setActionType(actionType);
        response.setMagazineId(magazineId);

        // [UX 최적화] 클라이언트가 바로 사용할 수 있도록 섹션 정렬
        magazine.getSections().sort((s1, s2) -> {
            Integer o1 = s1.getDisplayOrder() != null ? s1.getDisplayOrder() : Integer.MAX_VALUE;
            Integer o2 = s2.getDisplayOrder() != null ? s2.getDisplayOrder() : Integer.MAX_VALUE;
            return o1.compareTo(o2);
        });

        response.setMagazine(magazine); // [NEW] 업데이트된 매거진 설정
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
        // Python 응답: {intent, success, updated_magazine: {heading, content, image_url,
        // layout_hint}}
        String action = (String) response.get("intent"); // "intent" 필드 사용

        @SuppressWarnings("unchecked")
        Map<String, Object> updatedMagazine = (Map<String, Object>) response.get("updated_magazine");

        // [NEW] 단일 섹션 응답 이미지 S3 변환
        if (updatedMagazine != null) {
            String imageUrl = (String) updatedMagazine.get("image_url");
            if (imageUrl != null) {
                updatedMagazine.put("image_url", s3Service.uploadImageFromUrl(imageUrl));
            }
        }

        // [NEW] 전체 섹션 변경(change_tone) 이미지 S3 변환
        if ("change_tone".equals(action)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> newSections = (List<Map<String, Object>>) response.get("new_sections");
            if (newSections != null) {
                for (Map<String, Object> sec : newSections) {
                    String imgUrl = (String) sec.get("image_url");
                    if (imgUrl != null) {
                        sec.put("image_url", s3Service.uploadImageFromUrl(imgUrl));
                    }
                }
            }
        }

        // 1. 섹션 재생성
        if ("regenerate_section".equals(action)) {
            Integer sectionIndex = (Integer) response.get("section_index");

            if (sectionIndex != null && updatedMagazine != null && sectionIndex >= 0
                    && sectionIndex < magazine.getSections().size()) {
                MagazineSection section = magazine.getSections().get(sectionIndex);
                section.update(
                        (String) updatedMagazine.get("heading"),
                        (String) updatedMagazine.get("content"),
                        (String) updatedMagazine.get("image_url"),
                        (String) updatedMagazine.get("layout_hint"),
                        (String) updatedMagazine.get("layout_type"),
                        (String) updatedMagazine.get("caption"));
            }
        }

        // 2. 섹션 추가
        else if ("add_section".equals(action)) {
            if (updatedMagazine != null) {
                MagazineSection section = MagazineSection.builder()
                        .heading((String) updatedMagazine.get("heading"))
                        .content((String) updatedMagazine.get("content"))
                        .imageUrl((String) updatedMagazine.get("image_url"))
                        .layoutHint((String) updatedMagazine.get("layout_hint"))
                        .layoutType((String) updatedMagazine.get("layout_type"))
                        .caption((String) updatedMagazine.get("caption"))
                        .displayOrder(magazine.getSections().size()) // 맨 마지막에 추가
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

                // 삭제 후 순서 재정렬
                for (int i = 0; i < magazine.getSections().size(); i++) {
                    magazine.getSections().get(i).setDisplayOrder(i);
                }
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
                            .imageUrl((String) sec.get("image_url"))
                            .layoutHint((String) sec.get("layout_hint"))
                            .layoutType((String) sec.get("layout_type"))
                            .caption((String) sec.get("caption"))
                            .displayOrder(magazine.getSections().size()) // 순서대로 추가
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
