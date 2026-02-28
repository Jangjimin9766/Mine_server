package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineInteraction;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.Paragraph;
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

        response.setMagazine(com.mine.api.dto.MagazineDto.DetailResponse.from(magazine, false));
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
                    map.put("layout_hint", section.getLayoutHint());
                    map.put("layout_type", section.getLayoutType());
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

                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    protected void handlePythonResponse(Magazine magazine, Map<String, Object> response) {
        String action = (String) response.get("intent");
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedMagazine = (Map<String, Object>) response.get("updated_magazine");

        Integer sectionIndex = null;
        if (response.containsKey("section_index")) {
            Object idxObj = response.get("section_index");
            if (idxObj instanceof Number) {
                sectionIndex = ((Number) idxObj).intValue();
            }
        }

        // [NEW] Python V2 응답 구조 대응: updatedMagazine이 래퍼 객체이고 실제 데이터는 new_sections에 있음
        Map<String, Object> sectionData = updatedMagazine;
        List<Map<String, Object>> newSectionsList = null;
        List<Number> deletedSectionIds = null;

        if (updatedMagazine != null) {
            if (updatedMagazine.containsKey("new_sections")) {
                newSectionsList = (List<Map<String, Object>>) updatedMagazine.get("new_sections");
                if (newSectionsList != null && !newSectionsList.isEmpty()) {
                    sectionData = newSectionsList.get(0);
                }
            }
            if (updatedMagazine.containsKey("deleted_section_ids")) {
                deletedSectionIds = (List<Number>) updatedMagazine.get("deleted_section_ids");
            }
        }

        // S3 이미지 변환
        if (sectionData != null && !sectionData.containsKey("new_sections")) {
            uploadImagesInMap(sectionData);
        }
        if ("change_tone".equals(action) && newSectionsList != null) {
            for (Map<String, Object> sec : newSectionsList) {
                uploadImagesInMap(sec);
            }
        }

        // 1. 섹션 재생성
        if ("regenerate_section".equals(action)) {
            if (sectionIndex != null && sectionData != null && sectionIndex >= 0
                    && sectionIndex < magazine.getSections().size()) {
                MagazineSection section = magazine.getSections().get(sectionIndex);
                updateSectionFromMap(section, sectionData);
            }
        }

        // 2. 섹션 추가
        else if ("add_section".equals(action)) {
            if (sectionData != null) {
                MagazineSection section = createSectionFromMap(sectionData, magazine.getSections().size());
                section.setMagazine(magazine);
                magazine.getSections().add(section);
            }
        }

        // 3. 섹션 삭제
        else if ("delete_section".equals(action)) {
            boolean deleted = false;
            // V2 방식: deleted_section_ids 활용
            if (deletedSectionIds != null && !deletedSectionIds.isEmpty()) {
                Long targetId = deletedSectionIds.get(0).longValue();
                deleted = magazine.getSections().removeIf(s -> s.getId() != null && s.getId().equals(targetId));
            }
            // V1 방식: section_index 활용
            if (!deleted && sectionIndex != null && sectionIndex >= 0 && sectionIndex < magazine.getSections().size()) {
                magazine.getSections().remove(sectionIndex.intValue());
                deleted = true;
            }

            if (deleted) {
                // 삭제 후 순서 재정렬
                for (int i = 0; i < magazine.getSections().size(); i++) {
                    magazine.getSections().get(i).setDisplayOrder(i);
                }
            }
        }

        // 4. 전체 톤 변경 (모든 섹션 교체)
        else if ("change_tone".equals(action)) {
            if (newSectionsList != null) {
                magazine.getSections().clear();

                for (Map<String, Object> sec : newSectionsList) {
                    MagazineSection section = createSectionFromMap(sec, magazine.getSections().size());
                    section.setMagazine(magazine);
                    magazine.getSections().add(section);
                }
            }
        }

        // ⭐ 변경사항 DB에 저장
        magazineRepository.save(magazine);
    }

    private void uploadImagesInMap(Map<String, Object> sectionMap) {
        // thumbnail_url (or image_url fallback)
        String thumbUrl = (String) sectionMap.get("thumbnail_url");
        if (thumbUrl == null) {
            thumbUrl = (String) sectionMap.get("image_url"); // Legacy fallback
        }
        if (thumbUrl != null) {
            sectionMap.put("thumbnail_url", s3Service.uploadImageFromUrl(thumbUrl));
        }

        // paragraphs images
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paragraphs = (List<Map<String, Object>>) sectionMap.get("paragraphs");
        if (paragraphs != null) {
            for (Map<String, Object> p : paragraphs) {
                String imgUrl = (String) p.get("image_url");
                if (imgUrl != null) {
                    p.put("image_url", s3Service.uploadImageFromUrl(imgUrl));
                }
            }
        }
    }

    private void updateSectionFromMap(MagazineSection section, Map<String, Object> map) {
        section.update(
                (String) map.get("heading"),
                (String) map.get("layout_hint"),
                (String) map.get("layout_type"));

        // Update thumbnail
        if (map.get("thumbnail_url") != null) {
            section.setThumbnailUrl((String) map.get("thumbnail_url"));
        }

        // Update paragraphs
        section.getParagraphs().clear();
        addParagraphsFromMap(section, map);
    }

    private MagazineSection createSectionFromMap(Map<String, Object> map, int displayOrder) {
        String thumbUrl = (String) map.get("thumbnail_url");
        if (thumbUrl == null)
            thumbUrl = (String) map.get("image_url");

        MagazineSection section = MagazineSection.builder()
                .heading((String) map.get("heading"))
                .layoutHint((String) map.get("layout_hint"))
                .layoutType((String) map.get("layout_type"))
                .thumbnailUrl(thumbUrl)
                .displayOrder(displayOrder)
                .build();

        addParagraphsFromMap(section, map);
        return section;
    }

    private void addParagraphsFromMap(MagazineSection section, Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paragraphs = (List<Map<String, Object>>) map.get("paragraphs");

        if (paragraphs != null) {
            for (int i = 0; i < paragraphs.size(); i++) {
                Map<String, Object> pMap = paragraphs.get(i);
                Paragraph p = Paragraph.builder()
                        .subtitle((String) pMap.get("subtitle"))
                        .text((String) pMap.get("text"))
                        .imageUrl((String) pMap.get("image_url"))
                        .displayOrder(i)
                        .build();
                section.addParagraph(p);
            }
        } else {
            // Fallback for legacy AI response (flat content)
            String content = (String) map.get("content");
            if (content != null) {
                Paragraph p = Paragraph.builder()
                        .text(content)
                        .imageUrl((String) map.get("image_url"))
                        .subtitle((String) map.get("caption"))
                        .displayOrder(0)
                        .build();
                section.addParagraph(p);
            }
        }
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
