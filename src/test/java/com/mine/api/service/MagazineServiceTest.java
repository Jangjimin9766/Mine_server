package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.User;
import com.mine.api.domain.UserInterest;
import com.mine.api.domain.Interest;
import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.dto.MagazineGenerationRequest;
import com.mine.api.repository.MagazineLikeRepository;
import com.mine.api.repository.MagazineRepository;
import com.mine.api.repository.UserInterestRepository;
import com.mine.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MagazineServiceTest {

    @InjectMocks
    private MagazineService magazineService;

    @Mock
    private MagazineRepository magazineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MagazineLikeRepository magazineLikeRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(magazineService, "pythonApiUrl", "http://localhost:8000/api/magazine/generate");
    }

    @Test
    @DisplayName("AI 매거진 생성 및 저장 성공 테스트")
    void generateAndSaveMagazine_Success() {
        // Given
        String username = "testuser";
        MagazineGenerationRequest request = new MagazineGenerationRequest();
        request.setTopic("Travel");
        request.setUserMood("Happy");

        User user = User.builder()
                .username(username)
                .email("test@example.com")
                .password("password")
                .nickname("Tester")
                .build();

        UserInterest interest = UserInterest.builder()
                .user(user)
                .interest(Interest.TRAVEL)
                .build();

        MagazineCreateRequest generatedData = new MagazineCreateRequest();
        generatedData.setTitle("Generated Magazine");
        generatedData.setIntroduction("Intro");
        generatedData.setCoverImageUrl("http://image.url");

        Magazine savedMagazine = Magazine.builder()
                .user(user)
                .title("Generated Magazine")
                .build();
        ReflectionTestUtils.setField(savedMagazine, "id", 1L);

        // Mocking
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userInterestRepository.findByUser(user)).thenReturn(List.of(interest));
        when(restTemplate.postForObject(anyString(), any(), eq(MagazineCreateRequest.class)))
                .thenReturn(generatedData);
        when(magazineRepository.save(any(Magazine.class))).thenReturn(savedMagazine);

        // When
        Long magazineId = magazineService.generateAndSaveMagazine(request, username);

        // Then
        assertNotNull(magazineId);
        assertEquals(1L, magazineId);

        verify(userRepository, times(2)).findByUsername(username); // generateAndSaveMagazine calls it, saveMagazine
                                                                   // calls it too?
        // Wait, saveMagazine also calls findByUsername.
        // generateAndSaveMagazine calls:
        // 1. userRepository.findByUsername (line 78)
        // 2. restTemplate...
        // 3. saveMagazine(generatedData, username) -> calls
        // userRepository.findByUsername (line 28)

        verify(restTemplate).postForObject(anyString(), any(), eq(MagazineCreateRequest.class));
        verify(magazineRepository).save(any(Magazine.class));
    }
}
