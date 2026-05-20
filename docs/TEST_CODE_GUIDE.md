# 🧪 테스트 코드 작성 가이드

> **담당자**: (팀원 이름)  
> **기간**: ~2026년 2월 4일 (수)  
> **목표**: Mine-server 프로젝트의 테스트 커버리지 향상

---

## 📚 1. 테스트 코드란?

### 왜 테스트 코드를 작성할까요?

테스트 코드는 **우리가 작성한 코드가 올바르게 동작하는지 자동으로 확인**해주는 코드입니다.

```
🎯 테스트 코드가 없을 때:
   개발자 → 코드 수정 → 수동으로 하나하나 확인 → 실수 발생 가능 😰

✅ 테스트 코드가 있을 때:
   개발자 → 코드 수정 → 테스트 실행 → 버그 자동 발견! 🎉
```

### 현실적인 이점

1. **버그 조기 발견**: 프론트팀 연동 전에 문제를 찾을 수 있음
2. **리팩토링 안전망**: 코드를 수정해도 기존 기능이 깨지지 않았는지 확인
3. **문서 역할**: 테스트 코드를 보면 "이 기능이 어떻게 동작해야 하는지" 알 수 있음
4. **자신감**: 배포할 때 "테스트 다 통과했으니까 괜찮아" 라는 안도감 😌

---

## 🏗️ 2. 테스트의 종류

### 2.1 단위 테스트 (Unit Test) ⭐ 가장 중요

**하나의 메서드/클래스만** 테스트합니다.

```java
// UserService의 특정 메서드만 테스트
@Test
void 닉네임이_2자_미만이면_예외발생() {
    // given (준비)
    UserDto.UpdateRequest request = new UserDto.UpdateRequest("짧", null, null);
    
    // when & then (실행 및 검증)
    assertThrows(IllegalArgumentException.class, () -> {
        userService.updateProfile("testuser", request);
    });
}
```

### 2.2 통합 테스트 (Integration Test)

여러 컴포넌트가 **함께 잘 동작하는지** 테스트합니다.  
예: Controller → Service → Repository가 연결되어 동작하는지

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    
    @Test
    void 프로필_조회_성공() throws Exception {
        // 실제 Controller부터 DB까지 전체 흐름 테스트
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}
```

### 2.3 어떤 테스트를 먼저 작성할까?

```
우선순위:
1️⃣ Controller 통합 테스트 (API가 정상 호출되는지)
2️⃣ Service 단위 테스트 (비즈니스 로직이 올바른지)
3️⃣ Repository 테스트 (복잡한 쿼리가 있을 때만)
```

---

## 🛠️ 3. 프로젝트 테스트 구조

```
src/test/java/com/mine/api/
├── controller/          ← API 통합 테스트
│   ├── UserControllerTest.java
│   ├── MagazineControllerTest.java
│   └── ...
├── service/             ← 비즈니스 로직 단위 테스트
│   ├── UserServiceTest.java
│   ├── MagazineServiceTest.java
│   └── ...
└── repository/          ← 복잡한 쿼리 테스트 (필요시)
```

---

## 🔧 4. 테스트 작성 방법

### 4.1 테스트 실행해보기

먼저 기존 테스트가 잘 동작하는지 확인해보세요:

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "UserControllerTest"

# 특정 테스트 메서드만 실행
./gradlew test --tests "UserControllerTest.프로필_조회_성공"
```

### 4.2 Controller 테스트 작성하기

```java
package com.mine.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;  // 가짜 HTTP 요청을 보내주는 도구

    @Test
    @DisplayName("전체 관심사 조회 - 로그인 없이 성공")
    void 전체_관심사_조회_성공() throws Exception {
        // when & then
        mockMvc.perform(get("/api/interests"))
                .andExpect(status().isOk())                    // HTTP 200
                .andExpect(jsonPath("$").isArray())            // 배열인지 확인
                .andExpect(jsonPath("$[0].code").exists());    // code 필드 존재
    }
}
```

### 4.3 인증이 필요한 API 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 전에 로그인해서 토큰 획득
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "shared_user",
                        "password": "password"
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // JSON에서 토큰 추출 (ObjectMapper 사용)
        accessToken = new ObjectMapper()
                .readTree(loginResponse)
                .get("accessToken")
                .asText();
    }

    @Test
    @DisplayName("내 프로필 조회 - 성공")
    void 내_프로필_조회_성공() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("shared_user"))
                .andExpect(jsonPath("$.isPublic").exists())      // isPublic 필드 확인
                .andExpect(jsonPath("$.interests").isArray());   // interests 배열 확인
    }

    @Test
    @DisplayName("프로필 수정 - 닉네임 변경 성공")
    void 프로필_수정_성공() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nickname": "변경된닉네임"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("변경된닉네임"));
    }

    @Test
    @DisplayName("프로필 조회 - 토큰 없으면 401")
    void 프로필_조회_인증없음_실패() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());  // HTTP 401
    }
}
```

---

## 📋 5. 이번 주 과제: 테스트할 API 목록

아래 API들의 테스트 코드를 작성해주세요:

### 필수 (Must Have) 🔴

| API | 테스트 시나리오 |
|-----|----------------|
| `GET /api/interests` | 로그인 없이 조회 성공 |
| `GET /api/users/me` | 프로필 조회 성공, 토큰 없으면 401 |
| `PATCH /api/users/me` | 닉네임 수정 성공, 잘못된 닉네임 실패 |
| `GET /api/magazines/feed` | 추천 피드 조회, `hasNext`/`nextCursor` 포함 확인 |
| `GET /api/magazines/public` | 공개 계정 매거진 목록 조회 성공 |

### 권장 (Should Have) 🟡

| API | 테스트 시나리오 |
|-----|----------------|
| `PATCH /api/users/me/visibility` | 계정 공개/비공개 전환 성공 |
| `GET /api/magazines/{id}` | 내 매거진 조회, 다른 사람 매거진 403 |

### 선택 (Nice to Have) 🟢

| API | 테스트 시나리오 |
|-----|----------------|
| `POST /api/magazines` | 매거진 생성 성공 |
| `PATCH /api/magazines/{id}/sections/{sectionId}` | 섹션 수정 성공 |

---

## 💡 6. 테스트 작성 팁

### Given-When-Then 패턴

```java
@Test
void 계정_비공개_설정_성공() throws Exception {
    // Given: 준비 - 로그인된 사용자
    String token = getAccessToken();
    
    // When: 실행 - 비공개 설정 요청
    mockMvc.perform(patch("/api/users/me/visibility")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"isPublic\": false}"))
            
    // Then: 검증 - 성공 응답
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isPublic").value(false));
}
```

### 테스트 메서드 이름은 한글로!

```java
// ✅ 좋은 예
@Test
void 닉네임이_2자_미만이면_예외발생() { ... }

// ❌ 나쁜 예
@Test
void test1() { ... }
```

### 실패 케이스도 테스트!

```java
// 성공 케이스
@Test void 로그인_성공() { ... }

// 실패 케이스도 작성!
@Test void 잘못된_비밀번호로_로그인_실패() { ... }
@Test void 존재하지_않는_사용자_로그인_실패() { ... }
```

---

## 🚀 7. 시작하기

### Step 1: 기존 테스트 구경하기

```bash
# 기존 테스트 파일들 확인
ls src/test/java/com/mine/api/controller/
```

기존 `UserControllerTest.java`, `MagazineControllerTest.java` 등을 참고하세요!

### Step 2: 테스트 실행해보기

```bash
./gradlew test
```

### Step 3: 새 테스트 추가하기

`src/test/java/com/mine/api/controller/` 폴더에 새 테스트 클래스를 만들거나,  
기존 클래스에 테스트 메서드를 추가하세요.

### Step 4: PR 올리기

테스트 작성이 완료되면 PR을 올려주세요. 리뷰 후 머지하겠습니다!

---

## 📞 도움이 필요하면

- **질문**: Slack 채널에 언제든 물어보세요
- **참고 문서**: [Spring Boot Testing 공식 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- **기존 코드 참고**: `src/test/java/com/mine/api/` 폴더의 기존 테스트들

---

**화이팅! 💪**
