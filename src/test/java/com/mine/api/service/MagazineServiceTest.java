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
import com.mine.api.repository.FollowRepository;
import com.mine.api.domain.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashMap;
import java.util.Map;

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
        private FollowRepository followRepository;

        @Mock
        private RunPodService runPodService;

        // 테스트용 Interest 엔티티 생성 헬퍼
        private Interest createInterest(Long id, String code, String name) {
                Interest interest = Interest.builder()
                                .code(code)
                                .name(name)
                                .category("활동")
                                .build();
                ReflectionTestUtils.setField(interest, "id", id);
                return interest;
        }

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(magazineService, "pythonApiUrl",
                                "http://localhost:8000/api/magazine/generate");
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

                Interest travelInterest = createInterest(1L, "TRAVEL", "여행");
                UserInterest interest = UserInterest.builder()
                                .user(user)
                                .interest(travelInterest)
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

                Map<String, Object> outputMap = new HashMap<>();
                outputMap.put("title", "Generated Magazine");
                outputMap.put("introduction", "Intro");
                outputMap.put("cover_image_url", "http://image.url");

                // 로컬 URL이므로 sendSyncRequest가 호출됨
                when(runPodService.sendSyncRequest(anyString(), any(Map.class))).thenReturn(outputMap);
                when(magazineRepository.save(any(Magazine.class))).thenReturn(savedMagazine);

                // When
                Long magazineId = magazineService.generateAndSaveMagazine(request, username);

                // Then
                assertNotNull(magazineId);
                assertEquals(1L, magazineId);

                verify(userRepository, times(2)).findByUsername(username);
                verify(runPodService).sendSyncRequest(anyString(), any(Map.class));
                verify(magazineRepository).save(any(Magazine.class));
        }

        /*
         * @Test
         * 
         * @DisplayName("개인화 피드 조회 성공 테스트")
         * void getPersonalizedFeed_Success() {
         * // Given
         * String username = "testuser";
         * User user = User.builder().username(username).build();
         * User followingUser = User.builder().username("following").build();
         * Follow follow =
         * Follow.builder().follower(user).following(followingUser).build();
         * 
         * Interest travelInterest = createInterest(1L, "TRAVEL", "여행");
         * UserInterest interest =
         * UserInterest.builder().user(user).interest(travelInterest).build();
         * 
         * Pageable pageable = PageRequest.of(0, 10);
         * Page<Magazine> page = new PageImpl<>(List.of());
         * 
         * when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
         * when(followRepository.findByFollower(user)).thenReturn(List.of(follow));
         * when(userInterestRepository.findByUser(user)).thenReturn(List.of(interest));
         * when(magazineRepository.findPersonalizedFeed(anyList(), anyString(),
         * eq(pageable))).thenReturn(page);
         * 
         * // When
         * org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>
         * result = magazineService
         * .getPersonalizedFeed(username, pageable);
         * 
         * // Then
         * assertNotNull(result);
         * verify(magazineRepository).findPersonalizedFeed(anyList(), eq("여행"),
         * eq(pageable));
         * }
         * 
         * @Test
         * 
         * @DisplayName("개인화 피드 - 관심사와 팔로잉이 없는 경우")
         * void getPersonalizedFeed_NoInterests_NoFollowings() {
         * // Given
         * String username = "lonelyuser";
         * User user = User.builder().username(username).build();
         * 
         * Pageable pageable = PageRequest.of(0, 10);
         * Page<Magazine> page = new PageImpl<>(List.of());
         * 
         * when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
         * when(followRepository.findByFollower(user)).thenReturn(List.of()); // Empty
         * followings
         * when(userInterestRepository.findByUser(user)).thenReturn(List.of()); // Empty
         * interests
         * when(magazineRepository.findPersonalizedFeed(anyList(), eq(""),
         * eq(pageable))).thenReturn(page);
         * 
         * // When
         * org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem>
         * result = magazineService
         * .getPersonalizedFeed(username, pageable);
         * 
         * // Then
         * assertNotNull(result);
         * verify(magazineRepository).findPersonalizedFeed(eq(List.of()), eq(""),
         * eq(pageable));
         * }
         */

        @Test
        @DisplayName("좋아요 토글 - 동시성 문제 (DataIntegrityViolationException) 발생 시 처리")
        void toggleLike_Concurrency() {
                // Given
                String username = "testuser";
                Long magazineId = 1L;
                User user = User.builder().username(username).build();
                Magazine magazine = Magazine.builder().user(user).build();

                when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
                when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(magazine));
                when(magazineLikeRepository.findByUserAndMagazine(user, magazine)).thenReturn(Optional.empty());
                when(magazineLikeRepository.save(any(com.mine.api.domain.MagazineLike.class)))
                                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                                                "Duplicate entry"));

                // When
                boolean result = magazineService.toggleLike(magazineId, username);

                // Then
                assertTrue(result);
        }

        @Test
        @DisplayName("특정 유저의 공개 매거진 목록 조회 - 공개 계정일 경우 findAllByUserId 호출")
        void getPublicMagazines_WithUserId_Success() {
                // Given
                Long userId = 3L;
                User user = User.builder().username("testuser").build();
                ReflectionTestUtils.setField(user, "id", userId);
                user.setPublic(true); // 공개 계정 설정

                Pageable pageable = PageRequest.of(0, 10);
                Page<Magazine> page = new PageImpl<>(List.of());

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(magazineRepository.findAllByUserId(userId, pageable)).thenReturn(page);

                // When
                org.springframework.data.domain.Page<com.mine.api.dto.MagazineDto.ListItem> result = magazineService
                                .getPublicMagazines(userId, pageable);

                // Then
                assertNotNull(result);
                verify(userRepository).findById(userId);
                verify(magazineRepository).findAllByUserId(userId, pageable);
        }

        @Test
        @DisplayName("특정 유저의 공개 매거진 목록 조회 - 비공개 계정일 경우 예외 발생")
        void getPublicMagazines_WithUserId_PrivateAccount() {
                // Given
                Long userId = 3L;
                User user = User.builder().username("privateuser").build();
                user.setPublic(false); // 비공개 계정 설정

                Pageable pageable = PageRequest.of(0, 10);

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // When & Then
                assertThrows(SecurityException.class, () -> magazineService.getPublicMagazines(userId, pageable));
                verify(userRepository).findById(userId);
                verify(magazineRepository, never()).findAllByUserId(any(), any());
        }
}
