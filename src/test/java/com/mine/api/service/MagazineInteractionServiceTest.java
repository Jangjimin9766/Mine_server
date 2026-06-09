package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.repository.MagazineInteractionRepository;
import com.mine.api.repository.MagazineRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagazineInteractionServiceTest {

    @Test
    void failedAddSectionResponseDoesNotCreateSectionFromMessageWrapper() {
        MagazineInteractionService service = new MagazineInteractionService(
                mock(MagazineRepository.class),
                mock(MagazineInteractionRepository.class),
                mock(RunPodService.class),
                mock(S3Service.class),
                mock(SectionService.class),
                mock(ContentSafetyService.class));
        Magazine magazine = Magazine.builder().build();
        Map<String, Object> response = Map.of(
                "intent", "add_section",
                "success", false,
                "updated_magazine", Map.of(
                        "heading", "검증 가능한 검색 결과가 없어 구체적인 추천 대상을 생성하지 않았습니다.",
                        "new_sections", List.of(),
                        "deleted_section_ids", List.of()));

        service.handlePythonResponse(magazine, response);

        assertThat(magazine.getSections()).isEmpty();
    }
}
