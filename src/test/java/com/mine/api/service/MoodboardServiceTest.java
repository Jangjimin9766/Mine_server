package com.mine.api.service;

import com.mine.api.domain.Moodboard;
import com.mine.api.domain.User;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.dto.MoodboardResponseDto;
import com.mine.api.repository.MoodboardRepository;
import com.mine.api.repository.UserRepository;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private S3Template s3Template;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moodboardService, "moodboardApiUrl",
                "http://localhost:8000/api/magazine/moodboard");
        ReflectionTestUtils.setField(moodboardService, "bucketName", "test-bucket");
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
        MoodboardResponseDto aiResponse = new MoodboardResponseDto(base64Image, "A cozy test image");

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        // Mock WebClient chain
        given(webClientBuilder.build()).willReturn(webClient);
        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(any(MoodboardRequestDto.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(MoodboardResponseDto.class)).willReturn(Mono.just(aiResponse));

        // When
        String resultUrl = moodboardService.createMoodboard(username, requestDto);

        // Then
        assertNotNull(resultUrl);
        // Check if it starts with the expected bucket URL
        String expectedPrefix = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";
        assertEquals(expectedPrefix, resultUrl.substring(0, expectedPrefix.length()));

        verify(s3Template).upload(eq("test-bucket"), anyString(), any(InputStream.class));
        verify(moodboardRepository).save(any(Moodboard.class));
    }
}
