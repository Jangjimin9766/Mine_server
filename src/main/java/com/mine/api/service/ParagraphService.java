package com.mine.api.service;

import com.mine.api.common.ErrorMessages;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.Paragraph;
import com.mine.api.dto.ParagraphDto;
import com.mine.api.domain.Magazine;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.MagazineSectionRepository;
import com.mine.api.repository.ParagraphRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class ParagraphService {

    private final ParagraphRepository paragraphRepository;
    private final MagazineSectionRepository sectionRepository;
    private final MagazineRepository magazineRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final RunPodService runPodService;

    @Value("${python.api.url}")
    private String pythonApiUrl;

    @Transactional
    public Long createParagraph(Long magazineId, Long sectionId, ParagraphDto.CreateRequest request, String username) {
        validateOwnership(magazineId, sectionId, username);

        MagazineSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        // 이미지 업로드 처리
        String imageUrl = request.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            imageUrl = s3Service.uploadImageFromUrl(imageUrl);
        }

        // 마지막 순서 계산 (항상 맨 밑에 추가)
        Integer maxOrder = paragraphRepository.findMaxDisplayOrderBySectionId(sectionId);
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        Paragraph paragraph = Paragraph.builder()
                .subtitle(request.getSubtitle())
                .text(request.getText())
                .imageUrl(imageUrl)
                .displayOrder(nextOrder)
                .build();

        section.addParagraph(paragraph);
        Paragraph savedParagraph = paragraphRepository.save(paragraph);

        log.info("Paragraph created: id={}, magazineId={}, sectionId={}, order={}, username={}", 
                savedParagraph.getId(), magazineId, sectionId, nextOrder, username);
        
        return savedParagraph.getId();
    }

    @Transactional
    public Long createParagraphWithAi(Long magazineId, Long sectionId, ParagraphDto.AiCreateRequest request, String username) {
        validateOwnership(magazineId, sectionId, username);

        MagazineSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        
        Magazine magazine = section.getMagazine();

        // 1. Python AI 서버 요청 준비
        Map<String, Object> data = new HashMap<>();
        data.put("action", "generate_paragraph");
        data.put("topic", magazine.getTitle());
        data.put("user_mood", "vibrant"); // 기본값 사용
        data.put("section_heading", section.getHeading());
        data.put("message", request.getMessage());
        
        // 문맥 제공: 기존 문단들과 중복되지 않게 함
        List<Map<String, Object>> existingParas = section.getParagraphs().stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("subtitle", p.getSubtitle());
                    pMap.put("text", p.getText());
                    return pMap;
                })
                .collect(java.util.stream.Collectors.toList());
        data.put("existing_paragraphs", existingParas);

        log.info("Requesting AI paragraph generation: magazineId={}, sectionId={}, message={}", 
                magazineId, sectionId, request.getMessage());

        Map<String, Object> responseBody;
        Map<String, Object> output;

        // 로컬 환경 vs RunPod 환경 분기
        if (pythonApiUrl.contains("localhost") || pythonApiUrl.contains("127.0.0.1")) {
            responseBody = runPodService.sendSyncRequest(pythonApiUrl, data);
            output = responseBody;
        } else {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("action", "generate_paragraph");
            inputData.put("data", data);
            responseBody = runPodService.sendRequest(pythonApiUrl, inputData);
            
            if (responseBody == null || !responseBody.containsKey("output")) {
                throw new RuntimeException("AI server response missing output field");
            }
            output = (Map<String, Object>) responseBody.get("output");
        }

        if (output == null) {
            throw new RuntimeException("Failed to generate paragraph from AI");
        }

        // 2. 결과 파싱 (AI 응답 형식: {subtitle, text, image_url})
        String subtitle = (String) output.get("subtitle");
        String text = (String) output.get("text");
        String imageUrl = (String) output.get("image_url");

        if (subtitle == null || subtitle.isBlank()) subtitle = "새로운 이야기";
        if (text == null || text.isBlank()) text = "내용 생성에 실패했습니다.";

        // 3. 이미지 S3 업로드
        if (imageUrl != null && !imageUrl.isBlank()) {
            imageUrl = s3Service.uploadImageFromUrl(imageUrl);
        }

        // 4. 마지막 순서 계산
        Integer maxOrder = paragraphRepository.findMaxDisplayOrderBySectionId(sectionId);
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        Paragraph paragraph = Paragraph.builder()
                .subtitle(subtitle)
                .text(text)
                .imageUrl(imageUrl)
                .displayOrder(nextOrder)
                .build();

        section.addParagraph(paragraph);
        Paragraph savedParagraph = paragraphRepository.save(paragraph);

        // AI가 새 source_url을 반환하면 섹션의 sourceUrl도 업데이트
        String sourceUrl = (String) output.get("source_url");
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            section.setSourceUrl(sourceUrl);
            sectionRepository.save(section);
        }

        log.info("AI Paragraph created: id={}, order={}, magazineId={}, username={}", 
                savedParagraph.getId(), nextOrder, magazineId, username);
        
        return savedParagraph.getId();
    }

    @Transactional
    public void deleteParagraph(Long magazineId, Long sectionId, Long paragraphId, String username) {
        validateOwnership(magazineId, sectionId, username);

        Paragraph paragraph = paragraphRepository.findById(paragraphId)
                .orElseThrow(() -> new IllegalArgumentException("Paragraph not found"));

        if (!paragraph.getMagazineSection().getId().equals(sectionId)) {
            throw new IllegalArgumentException("Paragraph does not belong to the specified section");
        }

        MagazineSection section = paragraph.getMagazineSection();
        section.removeParagraph(paragraph); // 연관관계 편의 메서드 사용 권장 (또는 리포지토리 직접 삭제)
        paragraphRepository.delete(paragraph);

        // 삭제 후 순서 재정렬 (선택 사항)
        reorderParagraphs(section);

        log.info("Paragraph deleted: id={}, magazineId={}, username={}", paragraphId, magazineId, username);
    }

    private void validateOwnership(Long magazineId, Long sectionId, String username) {
        MagazineSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        if (!section.getMagazine().getId().equals(magazineId)) {
            throw new IllegalArgumentException("Section does not belong to the specified magazine");
        }

        if (!section.getMagazine().getUser().getUsername().equals(username)) {
            throw new SecurityException(ErrorMessages.NOT_AUTHORIZED);
        }
    }

    private void reorderParagraphs(MagazineSection section) {
        List<Paragraph> paragraphs = paragraphRepository.findBySectionIdOrderByDisplayOrderAsc(section.getId());
        for (int i = 0; i < paragraphs.size(); i++) {
            paragraphs.get(i).setDisplayOrder(i);
        }
    }
}
