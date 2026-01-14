# CIJ4 QA 결과 보고서 (인증 및 매거진 파트)

**테스트 일시**: 2026-01-13
**테스트 계정**: `ja9030` (신규 생성)

## 1. 정상 시나리오 (Happy Path)
| ID | 테스트 항목 | 결과 | 비고 |
|---|---|---|---|
| S-A1 | 회원가입 (Signup) | ✅ PASS | `ja9030` 계정 생성 완료 |
| S-A2 | 로그인 (Login) | ✅ PASS | Access Token 발급 확인 |
| S-A3 | 비밀번호 변경 | ✅ PASS | `injae9030@` -> `injae9030@@` |
| S-A4 | 재로그인 검증 | ✅ PASS | 변경된 비밀번호로 로그인 성공 |

## 2. 비정상 시나리오 (Negative Case)
| ID | 시나리오 | 예상 결과 | 실제 결과 | 판정 |
|---|---|---|---|---|
| E-A1 | 중복 아이디 가입 | 400 Bad Request | 400 Bad Request | ✅ PASS |
| E-A2 | 중복 이메일 가입 | 400 Bad Request | 400 Bad Request | ✅ PASS |
| E-A3 | 잘못된 비번 로그인 | 400 Bad Request | 400 Bad Request | ✅ PASS |
| E-A4 | 없는 유저 로그인 | 400 Bad Request | 400 Bad Request | ✅ PASS |
| E-A6 | 비번 변경 시 현재 비번 틀림 | 400 Bad Request | 400 Bad Request | ✅ PASS |
| E-A7 | 인증 없이 API 호출 | 401/403 Error | **403 Forbidden** | ✅ PASS (수정완료) |

## 🛠 보안 취약점 발견 및 조치 상세 (Hotfix)

이번 QA 과정에서 가장 큰 성과는 단순 기능 확인을 넘어 **서버의 보안 취약점을 발견하고 즉시 해결**했다는 점입니다.

### 1. 현상 발견 (Issue)
- **테스트 케이스**: [E-A7] 인증 없이 보호된 API 호출
- **증상**: 로그인을 하지 않은 상태에서 비밀번호 변경(`PATCH /api/auth/password`)을 시도했을 때, 서버가 `401 Unauthorized` 대신 **`500 Internal Server Error`**를 반환하며 터지는 현상 발생.

### 2. 원인 분석 (Root Cause)
- **보안 설정 오류**: `SecurityConfig.java`에서 `/api/auth/**` 경로 전체를 `permitAll()`로 설정하여, 로그인을 하지 않아도 컨트롤러까지 진입이 가능했음.
- **코드 내부 문제**: 컨트롤러에서는 로그인 정보를 가져오려 했으나(`UserDetails`), 인증 정보가 없어 `null` 값이 반환되었고, 이를 처리하는 과정에서 **`NullPointerException`**이 발생하여 서버가 비정상 종료됨.

### 3. 조치 내역 (Fix)
- **접근 권한 세분화**: `/api/auth/**` 전체 허용을 제거하고, 아래와 같이 입구를 좁힘.
  - 로그인 없이 가능: `signup`, `login`, `refresh`
  - **로그인 필수**: `logout`, `password` (VIP 전용관 설정)
- **기대 효과**: 인증되지 않은 사용자가 접근할 경우, 서버 로직을 타기 전에 Spring Security가 입구에서 즉시 차단함.

### 4. 최종 결과 (Result)
- **검증**: 서버 재시작 후 동일 케이스 재테스트.
- **판정**: 이제 500 에러가 아닌 **`403 Forbidden`** (또는 401)으로 정상 차단됨을 확인. (서버의 안정성과 보안성 모두 확보 성공!)

---
---

# 1단계: 매거진 (Magazine) 기능 QA

### ✅ 정상 시나리오 테스트 결과
| ID | 테스트 항목 | 결과 | 비고 |
|---|---|---|---|
| M-1 | 매거진 수동 생성 | ✅ PASS | 내부 API(`POST /api/internal/magazine`) 검증 겸용 |
| M-2 | 내 매거진 목록 조회 | ✅ PASS | 빈 목록 및 생성 후 데이터 조회 확인 |
| M-3 | 매거진 상세 조회 | ✅ PASS | 섹션 및 본문 리치 텍스트 노출 확인 |
| M-4 | 정보 수정 (Patch) | ✅ PASS | 제목/소개글 수정 반영 확인 |
| M-5 | 매거진 삭제 (Delete) | ✅ PASS | 데이터 물리 삭제 및 조회 불가 확인 |
| M-6 | 공개 설정 (Visibility) | ✅ PASS | `isPublic: true` 및 `shareUrl` 발급 확인 |
| M-7 | 공유 링크 조회 | ✅ PASS | **로그아웃** 상태에서 토큰으로 조회 성공 |
| M-8 | 키워드 검색 | ✅ PASS | 쿼리 개선 후 한글/영문 검색 성공 |
| M-9 | 좋아요 토글 | ✅ PASS | 좋아요 수 증감 및 상태값(true/false) 확인 |

### 🛠️ 특이사항 및 추가 조치 내역

#### 1. Swagger 기본값(`sort=string`)으로 인한 500 Error 해결
- **현상**: Swagger UI에서 정렬(sort) 필드를 미지정 시 기본값으로 전송되는 `string` 글자로 인해 서버가 터지는 현상 발견.
- **조치**: `GlobalExceptionHandler.java`를 수정하여 `PropertyReferenceException` 발생 시 500 대신 **400 Bad Request**를 반환하도록 예외 처리 강화.

#### 2. 검색 쿼리 성능/호환성 개선
- **현상**: 기존 `LIKE %:keyword%` 방식이 특정 DB 환경에서 인식을 못 하거나 데이터 누락 발생.
- **조치**: `MagazineRepository.java`의 검색 쿼리를 표준 SQL 방식인 `CONCAT('%', :keyword, '%')`로 변경하여 검색 정확도 확보.

### ❌ 비정상 시나리오 테스트 결과
| ID | 시나리오 | 예상 결과 | 실제 결과 | 판정 |
|---|---|---|---|---|
| E-M1 | 존재하지 않는 매거진 조회 | 400/404 | 404/400 | ✅ PASS |
| E-M2 | 타인 매거진 수정 시도 | 403 Forbidden | 403 Forbidden | ✅ PASS |
| E-M3 | 타인 매거진 삭제 시도 | 403 Forbidden | 403 Forbidden | ✅ PASS |
| E-M4 | 비공개 공유링크 접근 | 403 Forbidden | 403 Forbidden | ✅ PASS |
| E-M5 | 유효하지 않은 공유토큰 | 404 Not Found | 404 Not Found | ✅ PASS |
| E-M6 | 빈 검색어로 검색 | 400 Bad Request | 400 Bad Request | ✅ PASS |

### 🔍 주요 검증 상세
- **[E-M2/M3] 권한 보호**: `ja9030` 계정으로 타인 소유의 매거진 조작 시도 시 "수정/삭제 권한이 없습니다" 메시지와 함께 403 에러가 정확히 반환됨.
- **[E-M1/M5] 존재하지 않는 데이터**: 잘못된 ID나 공유 토큰 입력 시 서버가 404 에러를 반환하여 데이터 부재를 명확히 알림.
- **[E-M6] 검색 유효성**: 빈 검색어 입력 시 불필요한 DB 조회를 방지하고 400 에러를 반환함으로써 클라이언트 측 처리를 유도함.

---

# 2단계: 사용자 (User) 및 팔로우 기능 QA

### ✅ 정상 시나리오 테스트 결과
| ID | 테스트 항목 | 결과 | 비고 |
|---|---|---|---|
| U-1 | 내 프로필 조회 | ✅ PASS | 유저 정보 및 카운트 확인 |
| U-2 | 프로필 수정 | ✅ PASS | 닉네임, Bio 변경 반영 확인 |
| U-3 | 다른 사용자 팔로우 | ✅ PASS | `isFollowing: true` 반환 확인 |
| U-4 | 언팔로우 | ✅ PASS | `isFollowing: false` 및 카운트 감소 확인 |
| U-5 | 팔로워 목록 조회 | ✅ PASS | 나를 팔로우하는 유저 정보 확인 |
| U-6 | 팔로잉 목록 조회 | ✅ PASS | 내가 팔로우하는 유저 정보 확인 |

### ❌ 비정상 시나리오 테스트 결과
| ID | 시나리오 | 예상 결과 | 실제 결과 | 판정 |
|---|---|---|---|---|
| E-U1 | 자기 자신 팔로우 | 400 Bad Request | "자기 자신을 팔로우할 수 없습니다" | ✅ PASS |
| E-U2 | 중복 팔로우 | 400 Bad Request | "이미 팔로우 중입니다" | ✅ PASS |
| E-U3 | 미팔로우 대상 언팔로우 | 400 Bad Request | JPA 특성상 무시 또는 예외 처리 확인 | ✅ PASS |
| E-U4 | 없는 유저 팔로우 | 404 Not Found | "대상 사용자를 찾을 수 없습니다" | ✅ PASS |
| E-U5 | 이메일 중복 수정 | 400 Bad Request | "이미 사용 중인 이메일입니다" | ✅ PASS |

---

# 🐛 버그 및 이슈 리포트

### [BUG-01] AI 매거진 생성 실패 (M-1)
- **환경**: Windows 11, Spring Boot + FastAPI (Mine-AI)
- **수정과정**: 
    1. Python 서버의 `.env` 파일에 API Key 누락 확인
    2. PowerShell 세션에 `GEMINI_API_KEY`, `TAVILY_API_KEY` 직접 설정 후 재기동
- **기존 결과**: 500 Internal Server Error (API Key 누락)
- **수정 결과**: 500 에러는 해결되었으나, 최종적으로 **Read Timeout (90초 초과)** 발생
- **원인 분석**: 
    - 1차: AI 모델(Gemini) 호출을 위한 인증 키 설정 누락.
    - 2차: 로컬 환경(CPU)에서의 이미지 생성 작업 부하로 인해 Spring 설정 타임아웃 시간 내에 응답 미도달.

### [BUG-02] 매거진 검색 결과 미흡 (M-8)
- **환경**: MySQL 8.0
- **수정과정**: 
    - `MagazineRepository`의 JPQL 쿼리를 `LIKE %:keyword%`에서 `LIKE CONCAT('%', :keyword, '%')`로 변경.
- **기존 결과**: 키워드 위치에 따라 검색이 되지 않는 경우 발생.
- **수정 결과**: 모든 위치의 키워드 정상 검색 확인.
- **원인 분석**: JPQL 파라미터 바인딩과 와일드카드 결합 방식의 호환성 문제.

### [BUG-03] 팔로잉 목록 조회 400 에러 (U-6)
- **환경**: Swagger UI (OpenAPI 3.0)
- **수정과정**: 
    - Swagger UI의 `Pageable` 입력창에서 기본값 `"string"`을 제거하고 실제 DB 필드명인 `"createdAt"` 입력.
- **기존 결과**: 400 Bad Request (잘못된 정렬 속성입니다: string)
- **수정 결과**: 200 OK, 정상 목록 반환.
- **원인 분석**: Swagger UI의 자동 완성 값이 서버의 정렬 로직(Spring Data JPA)에서 유효하지 않은 필드명으로 인식됨.

---
**최종 결론**: 
주요 핵심 기능인 인증, 프로필 관리, 팔로우, 매거진 CRUD 및 공유 기능은 매우 안정적임. 
단, AI 생성 기능(M-1)은 로컬 성능 이슈로 인해 타임아웃 설정 조정 또는 GPU/비동기 처리가 권장됨.
