package com.mine.api.service;

import com.mine.api.domain.Paragraph;
import com.mine.api.repository.MagazineSectionRepository;
import com.mine.api.repository.ParagraphRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagazineImageUploadServiceTest {

    @Test
    @DisplayName("문단 이미지 업로드 실패 시 기존 외부 URL을 null로 정리한다")
    void uploadMagazineImagesAsync_ParagraphUploadFails_ClearsImageUrl() {
        S3Service s3Service = mock(S3Service.class);
        MagazineSectionRepository sectionRepository = mock(MagazineSectionRepository.class);
        ParagraphRepository paragraphRepository = mock(ParagraphRepository.class);
        MagazineImageUploadService service = new MagazineImageUploadService(
                s3Service,
                sectionRepository,
                paragraphRepository);

        Paragraph paragraph = Paragraph.builder()
                .subtitle("subtitle")
                .text("text")
                .imageUrl("https://example.com/broken.jpg")
                .displayOrder(0)
                .build();

        when(s3Service.uploadImageFromUrl("https://example.com/broken.jpg")).thenReturn(null);
        when(paragraphRepository.findById(1L)).thenReturn(Optional.of(paragraph));

        service.uploadMagazineImagesAsync(
                10L,
                List.of(),
                List.of(new MagazineImageUploadService.ParagraphImageUpload(1L, "https://example.com/broken.jpg")));

        assertNull(paragraph.getImageUrl());
        verify(paragraphRepository).save(paragraph);
    }
}
