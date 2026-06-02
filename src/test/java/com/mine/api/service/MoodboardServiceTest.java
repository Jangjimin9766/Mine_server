package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.MoodboardStatus;
import com.mine.api.domain.Paragraph;
import com.mine.api.domain.User;
import com.mine.api.domain.Interest;
import com.mine.api.domain.UserInterest;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.repository.MoodboardRepository;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MoodboardServiceTest {

    @InjectMocks
    private MoodboardService moodboardService;

    @Mock
    private MoodboardRepository moodboardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MagazineRepository magazineRepository;

    @Mock
    private com.mine.api.repository.UserInterestRepository userInterestRepository;

    @Mock
    private RunPodService runPodService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ContentSafetyService contentSafetyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moodboardService, "moodboardApiUrl",
                "http://localhost:8000/api/magazine/moodboard");
    }

    @Test
    void createMoodboard_Success() {
        // Given
        String username = "testUser";
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(1L);

        MoodboardRequestDto requestDto = MoodboardRequestDto.builder()
                .topic("Test Topic")
                .build();

        String base64Image = Base64.getEncoder().encodeToString("fake-image-content".getBytes());

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("image_url", base64Image);
        output.put("description", "A cozy test image");

        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);
        given(s3Service.uploadBase64ToS3(base64Image)).willReturn("https://test-bucket.s3.ap-southeast-2.amazonaws.com/moodboards/test.png");

        String resultUrl = moodboardService.createMoodboard(username, requestDto);

        assertNotNull(resultUrl);

        verify(s3Service).uploadBase64ToS3(base64Image);
        verify(moodboardRepository).save(any(Moodboard.class));
    }

    @Test
    void createMoodboard_SuccessFalse_DoesNotUseFallbackImage() {
        String username = "testUser";
        User user = org.mockito.Mockito.mock(User.class);
        MoodboardRequestDto requestDto = MoodboardRequestDto.builder()
                .topic("Test Topic")
                .build();

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("success", false);
        output.put("error_type", "SAFETY_BLOCKED");
        output.put("fallback_url", "fallback-image");

        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);

        assertThrows(RuntimeException.class, () -> moodboardService.createMoodboard(username, requestDto));
        org.mockito.Mockito.verify(s3Service, org.mockito.Mockito.never()).uploadBase64ToS3(anyString());
        org.mockito.Mockito.verify(moodboardRepository, org.mockito.Mockito.never()).save(any(Moodboard.class));
    }

    @Test
    void createMoodboardForMagazine_UpdatesMagazineStatus() {
        String username = "testUser";
        User user = User.builder().username(username).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Magazine magazine = Magazine.builder()
                .title("홈 오피스 생산성 셋업")
                .coverImageUrl("https://example.com/cover.jpg")
                .tags("TECH,INTERIOR")
                .user(user)
                .build();
        ReflectionTestUtils.setField(magazine, "id", 10L);
        MagazineSection section = MagazineSection.builder()
                .heading("집중을 돕는 조명과 책상 구성")
                .displayOrder(0)
                .build();
        section.addParagraph(Paragraph.builder()
                .subtitle("업무 루틴")
                .text("작은 책상과 따뜻한 조명이 생산성 있는 홈 오피스 분위기를 만든다.")
                .displayOrder(0)
                .build());
        magazine.addSection(section);

        Interest interest = Interest.builder().code("TECH").name("테크").build();
        UserInterest userInterest = UserInterest.builder().user(user).interest(interest).build();

        String base64Image = Base64.getEncoder().encodeToString("fake-magazine-image".getBytes());
        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("success", true);
        output.put("image_url", base64Image);
        output.put("description", "A clean home office moodboard");

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));
        given(magazineRepository.findById(10L)).willReturn(Optional.of(magazine));
        given(magazineRepository.findByIdAndUserUsername(10L, username)).willReturn(Optional.of(magazine));
        given(userInterestRepository.findByUser(user)).willReturn(java.util.List.of(userInterest));
        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);
        given(s3Service.uploadBase64ToS3(base64Image)).willReturn("https://test-bucket/moodboards/magazine.png");

        String resultUrl = moodboardService.createMoodboardForMagazine(10L, username);

        assertEquals("https://test-bucket/moodboards/magazine.png", resultUrl);
        assertEquals("https://test-bucket/moodboards/magazine.png", magazine.getMoodboardImageUrl());
        assertEquals("A clean home office moodboard", magazine.getMoodboardDescription());
        assertEquals(MoodboardStatus.COMPLETED, magazine.getMoodboardStatus());
        assertEquals("https://example.com/cover.jpg", magazine.getCoverImageUrl());

        verify(runPodService).sendSyncRequest(eq("http://localhost:8000/api/magazine/moodboard"), any(java.util.Map.class));
        verify(moodboardRepository).save(any(Moodboard.class));
    }

    @Test
    void createMoodboardForMagazine_SendsSectionContext() {
        String username = "testUser";
        User user = User.builder().username(username).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Magazine magazine = Magazine.builder()
                .title("영국 드라마 추천")
                .tags("BRITISH_DRAMA,OTT")
                .user(user)
                .build();
        ReflectionTestUtils.setField(magazine, "id", 10L);
        MagazineSection section = MagazineSection.builder()
                .heading("절제된 미스터리의 매력")
                .displayOrder(0)
                .build();
        section.addParagraph(Paragraph.builder()
                .subtitle("느린 호흡의 수사극")
                .text("영국 드라마는 차분한 색감, 미묘한 표정, 촘촘한 대사로 긴장감을 쌓아 간다.")
                .displayOrder(0)
                .build());
        magazine.addSection(section);

        String base64Image = Base64.getEncoder().encodeToString("fake-magazine-image".getBytes());
        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("success", true);
        output.put("image_url", base64Image);

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));
        given(magazineRepository.findById(10L)).willReturn(Optional.of(magazine));
        given(magazineRepository.findByIdAndUserUsername(10L, username)).willReturn(Optional.of(magazine));
        given(userInterestRepository.findByUser(user)).willReturn(java.util.List.of());
        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);
        given(s3Service.uploadBase64ToS3(base64Image)).willReturn("https://test-bucket/moodboards/magazine.png");

        moodboardService.createMoodboardForMagazine(10L, username);

        org.mockito.ArgumentCaptor<java.util.Map<String, Object>> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(runPodService).sendSyncRequest(anyString(), requestCaptor.capture());
        java.util.Map<String, Object> request = requestCaptor.getValue();

        assertEquals(java.util.List.of("절제된 미스터리의 매력"), request.get("section_headings"));
        org.junit.jupiter.api.Assertions.assertTrue(request.get("content_keywords").toString().contains("느린 호흡의 수사극"));
        org.junit.jupiter.api.Assertions.assertTrue(request.get("content_keywords").toString().contains("영국 드라마"));
    }

    @Test
    void createMoodboardForMagazine_FallbackOnlyMarksFailed() {
        String username = "testUser";
        User user = User.builder().username(username).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Magazine magazine = Magazine.builder()
                .title("영국 드라마 추천")
                .user(user)
                .build();
        ReflectionTestUtils.setField(magazine, "id", 10L);

        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("success", true);
        output.put("fallback_url", "https://example.com/generic-fallback.jpg");

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));
        given(magazineRepository.findById(10L)).willReturn(Optional.of(magazine));
        given(magazineRepository.findByIdAndUserUsername(10L, username)).willReturn(Optional.of(magazine));
        given(userInterestRepository.findByUser(user)).willReturn(java.util.List.of());
        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);

        assertThrows(RuntimeException.class, () -> moodboardService.createMoodboardForMagazine(10L, username));
        assertEquals(MoodboardStatus.FAILED, magazine.getMoodboardStatus());
        org.mockito.Mockito.verify(s3Service, org.mockito.Mockito.never()).uploadBase64ToS3(anyString());
    }

    @Test
    void createMoodboardForMagazine_FailureMarksFailed() {
        String username = "testUser";
        User user = User.builder().username(username).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Magazine magazine = Magazine.builder()
                .title("홈 오피스 생산성 셋업")
                .coverImageUrl("https://example.com/cover.jpg")
                .user(user)
                .build();
        ReflectionTestUtils.setField(magazine, "id", 10L);

        java.util.Map<String, Object> output = new java.util.HashMap<>();
        output.put("success", false);
        output.put("error_type", "IMAGE_GENERATION_FAILED");

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));
        given(magazineRepository.findById(10L)).willReturn(Optional.of(magazine));
        given(magazineRepository.findByIdAndUserUsername(10L, username)).willReturn(Optional.of(magazine));
        given(userInterestRepository.findByUser(user)).willReturn(java.util.List.of());
        given(runPodService.sendSyncRequest(anyString(), any(java.util.Map.class))).willReturn(output);

        assertThrows(RuntimeException.class, () -> moodboardService.createMoodboardForMagazine(10L, username));
        assertEquals(MoodboardStatus.FAILED, magazine.getMoodboardStatus());
        assertEquals("https://example.com/cover.jpg", magazine.getCoverImageUrl());
    }
}
