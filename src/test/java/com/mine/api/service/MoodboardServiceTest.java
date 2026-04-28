package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.domain.User;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
    private RunPodService runPodService;

    @Mock
    private S3Service s3Service;

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
}
