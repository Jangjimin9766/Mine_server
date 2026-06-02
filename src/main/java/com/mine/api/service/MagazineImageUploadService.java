package com.mine.api.service;

import com.mine.api.repository.MagazineSectionRepository;
import com.mine.api.repository.ParagraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagazineImageUploadService {

    private final S3Service s3Service;
    private final MagazineSectionRepository sectionRepository;
    private final ParagraphRepository paragraphRepository;

    public record SectionImageUpload(Long sectionId, String imageUrl) {
    }

    public record ParagraphImageUpload(Long paragraphId, String imageUrl) {
    }

    @Async
    @Transactional
    public void uploadMagazineImagesAsync(
            Long magazineId,
            List<SectionImageUpload> sectionImages,
            List<ParagraphImageUpload> paragraphImages) {
        log.info(
                "Starting async magazine image upload: magazineId={}, sectionImages={}, paragraphImages={}",
                magazineId,
                sectionImages.size(),
                paragraphImages.size());

        for (SectionImageUpload sectionImage : sectionImages) {
            try {
                String uploadedUrl = s3Service.uploadImageFromUrl(sectionImage.imageUrl());
                if (uploadedUrl != null) {
                    sectionRepository.findById(sectionImage.sectionId()).ifPresent(section -> {
                        section.setThumbnailUrl(uploadedUrl);
                        sectionRepository.save(section);
                    });
                }
            } catch (Exception e) {
                log.warn(
                        "Async section image upload failed: magazineId={}, sectionId={}, url={}",
                        magazineId,
                        sectionImage.sectionId(),
                        sectionImage.imageUrl(),
                        e);
            }
        }

        for (ParagraphImageUpload paragraphImage : paragraphImages) {
            try {
                String uploadedUrl = s3Service.uploadImageFromUrl(paragraphImage.imageUrl());
                paragraphRepository.findById(paragraphImage.paragraphId()).ifPresent(paragraph -> {
                    paragraph.setImageUrl(uploadedUrl);
                    paragraphRepository.save(paragraph);
                });
            } catch (Exception e) {
                log.warn(
                        "Async paragraph image upload failed: magazineId={}, paragraphId={}, url={}",
                        magazineId,
                        paragraphImage.paragraphId(),
                        paragraphImage.imageUrl(),
                        e);
            }
        }

        log.info("Finished async magazine image upload: magazineId={}", magazineId);
    }
}
