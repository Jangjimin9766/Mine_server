package com.mine.api.service;

import com.mine.api.common.ErrorMessages;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.Paragraph;
import com.mine.api.dto.ParagraphDto;
import com.mine.api.repository.MagazineSectionRepository;
import com.mine.api.repository.ParagraphRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class ParagraphService {

    private final ParagraphRepository paragraphRepository;
    private final MagazineSectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

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
