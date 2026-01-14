# M:ine API QA 테스트 가이드

> **테스트 기간**: 2024년 12월 25일 (수) ~ **12월 31일 (수) 자정까지**

## 📋 목차

- [시작하기](#-시작하기)
- [자동화 테스트 실행](#-자동화-테스트-실행)
- [Swagger UI로 수동 테스트](#-swagger-ui로-수동-테스트)
- [테스트 결과 확인](#-테스트-결과-확인)
- [버그 리포팅](#-버그-리포팅)
- [문제 해결](#-문제-해결)

---

## 🚀 시작하기

### 1. 사전 준비

테스트를 시작하기 전에 다음 사항을 확인하세요:

```powershell
# 1. M:ine 서버가 실행 중인지 확인
# 서버 실행 (프로젝트 루트에서)
./gradlew bootRun

# 2. 서버 접속 확인
# 브라우저에서 http://localhost:8080/swagger-ui.html 접속
```

✅ **체크리스트:**
- [ ] MySQL 8.0 실행 중
- [ ] Redis 실행 중
- [ ] M:ine 서버 실행 중 (포트 8080)
- [ ] Swagger UI 접속 가능
- [ ] (선택) Python AI 서버 실행 중 (포트 8000) - 무드보드 테스트용

### 2. 테스트 스크립트 확인

프로젝트 루트 디렉토리에 다음 파일들이 있는지 확인:

```
Mine_server/
├── qa_test_normal.ps1      # 정상 시나리오 테스트
├── qa_test_errors.ps1       # 에러 시나리오 테스트
├── qa_test_runner.ps1       # 통합 테스트 실행기
├── QA_TEST_GUIDE.md         # 이 파일
└── BUG_REPORT_TEMPLATE.md   # 버그 리포트 템플릿
```

---

## 🤖 자동화 테스트 실행

### 방법 1: 전체 테스트 실행 (권장)

모든 테스트를 한 번에 실행합니다:

```powershell
# PowerShell에서 실행
.\qa_test_runner.ps1
```

### 방법 2: 개별 테스트 실행

특정 테스트만 실행하고 싶을 때:

```powershell
# 정상 시나리오만 테스트
.\qa_test_normal.ps1

# 에러 시나리오만 테스트
.\qa_test_errors.ps1
```

### 실행 옵션

```powershell
# 정상 시나리오만 실행
.\qa_test_runner.ps1 -NormalOnly

# 에러 시나리오만 실행
.\qa_test_runner.ps1 -ErrorsOnly
```

### 실행 권한 문제 해결

PowerShell 스크립트 실행 시 권한 오류가 발생하면:

```powershell
# 실행 정책 확인
Get-ExecutionPolicy

# 실행 정책 변경 (관리자 권한 필요)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 또는 일회성 실행
PowerShell -ExecutionPolicy Bypass -File .\qa_test_runner.ps1
```

---

## 🌐 Swagger UI로 수동 테스트

자동화 테스트 외에 Swagger UI를 사용한 수동 테스트도 권장합니다.

### 1. Swagger UI 접속

브라우저에서 다음 URL로 이동:
```
http://localhost:8080/swagger-ui.html
```

### 2. 인증 설정

대부분의 API는 인증이 필요합니다:

#### Step 1: 회원가입
1. **인증 (Authentication)** 섹션 펼치기
2. `POST /api/auth/signup` 클릭
3. "Try it out" 버튼 클릭
4. Request body 입력:
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test1234!@",
  "nickname": "테스터"
}
```
5. "Execute" 클릭

#### Step 2: 로그인
1. `POST /api/auth/login` 클릭
2. Request body 입력:
```json
{
  "username": "testuser",
  "password": "Test1234!@"
}
```
3. "Execute" 클릭
4. Response에서 `accessToken` 복사

#### Step 3: 인증 토큰 설정
1. Swagger UI 상단의 **"Authorize"** 버튼 클릭
2. Value 필드에 입력: `Bearer {복사한_accessToken}`
3. "Authorize" 클릭
4. "Close" 클릭

이제 인증이 필요한 모든 API를 테스트할 수 있습니다!

### 3. 테스트 케이스별 실행

#### 정상 시나리오 예시

**A-1: 회원가입**
- Endpoint: `POST /api/auth/signup`
- 예상 결과: 200 OK, 사용자 ID 반환

**M-1: 매거진 생성**
- Endpoint: `POST /api/internal/magazine`
- Headers: `X-Internal-Key: mine-secret-key-1234`
- 예상 결과: 200 OK, magazineId 반환

**U-3: 팔로우**
- Endpoint: `POST /api/users/{userId}/follow`
- 예상 결과: 200 OK, isFollowing: true

#### 에러 시나리오 예시

**E-A1: 중복 아이디 회원가입**
- 이미 존재하는 username으로 회원가입 시도
- 예상 결과: 400 Bad Request

**E-M2: 권한 없는 매거진 수정**
- 다른 사용자의 매거진 ID로 수정 시도
- 예상 결과: 403 Forbidden

**E-U1: 자기 자신 팔로우**
- 본인 userId로 팔로우 시도
- 예상 결과: 400 Bad Request

---

## 📊 테스트 결과 확인

### 자동화 테스트 결과

테스트 실행 후 다음 파일들이 생성됩니다:

```
test_results_normal_YYYYMMDDHHMMSS.json
test_results_errors_YYYYMMDDHHMMSS.json
test_results_errors_YYYYMMDDHHMMSS.csv
```

### 결과 파일 확인

#### JSON 파일
```powershell
# JSON 파일 보기
Get-Content test_results_normal_*.json | ConvertFrom-Json | Format-Table
```

#### CSV 파일 (Excel로 열기)
```powershell
# CSV 파일을 Excel로 열기
Invoke-Item test_results_errors_*.csv
```

### 결과 해석

각 테스트 결과는 다음 정보를 포함합니다:

| 필드 | 설명 |
|------|------|
| TestId | 테스트 케이스 ID (예: A-1, E-M2) |
| Description | 테스트 설명 |
| Status | PASS 또는 FAIL |
| StatusCode | HTTP 상태 코드 |
| Message | 추가 메시지 |

#### 정상 시나리오 결과
- **PASS**: 예상대로 성공 (200, 204 등)
- **FAIL**: 예상과 다른 결과 → **버그 가능성**

#### 에러 시나리오 결과
- **PASS**: 예상된 에러 발생 (400, 401, 403, 404 등)
- **FAIL**: 예상과 다른 결과 → **버그 가능성**

---

## 🐛 버그 리포팅

### 버그 발견 시

테스트에서 **FAIL**이 발생하거나 예상과 다른 동작을 발견하면:

1. **스크린샷 캡처** (Swagger UI 또는 터미널 출력)
2. **에러 로그 복사**
3. **GitHub Issue 생성**

### GitHub Issue 등록

1. Repository 이동: [Mine_server Issues](https://github.com/Jangjimin9766/Mine_server/issues)
2. "New Issue" 클릭
3. `BUG_REPORT_TEMPLATE.md` 참고하여 작성
4. Label 추가: `bug`

### Issue 작성 예시

```markdown
## 🐛 버그 리포트

### 테스트 케이스 ID
E-M2

### 환경
- OS: Windows 11
- 테스트 도구: PowerShell 자동화 스크립트

### 재현 방법
1. 사용자 A로 로그인
2. 사용자 B의 매거진 ID로 수정 시도
3. PUT /api/magazines/{id} 호출

### 예상 결과
403 Forbidden

### 실제 결과
200 OK - 수정이 성공함 (권한 체크 누락)

### 스크린샷/로그
[스크린샷 첨부]

### 추가 정보
다른 사용자의 매거진을 수정할 수 있는 보안 취약점
```

---

## 🔧 문제 해결

### 서버 연결 실패

```
❌ 서버에 연결할 수 없습니다!
```

**해결 방법:**
1. M:ine 서버가 실행 중인지 확인
```powershell
# 서버 실행
./gradlew bootRun
```

2. 포트 8080이 사용 중인지 확인
```powershell
# Windows
netstat -ano | findstr :8080
```

### MySQL 연결 실패

```
Could not open JDBC Connection
```

**해결 방법:**
1. MySQL 서비스 확인
```powershell
# MySQL 상태 확인 (Windows)
Get-Service -Name MySQL*
```

2. 데이터베이스 존재 확인
```sql
SHOW DATABASES LIKE 'mine_db';
```

### Redis 연결 실패

```
Unable to connect to Redis
```

**해결 방법:**
1. Redis 서비스 확인
```powershell
# Redis 실행 (Docker)
docker run -d -p 6379:6379 redis

# 또는 로컬 Redis 실행
redis-server
```

2. Redis 연결 테스트
```powershell
redis-cli ping
# 응답: PONG
```

### 테스트 스크립트 실행 권한 오류

```
cannot be loaded because running scripts is disabled
```

**해결 방법:**
```powershell
# 실행 정책 변경
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 또는 일회성 실행
PowerShell -ExecutionPolicy Bypass -File .\qa_test_runner.ps1
```

### Python AI 서버 연결 실패 (무드보드 테스트)

```
Connection refused to localhost:8000
```

**해결 방법:**
1. Python AI 서버는 선택사항입니다
2. 무드보드 테스트(B-1)만 실패하고 나머지는 정상 진행됩니다
3. AI 서버 없이도 대부분의 테스트 가능

---

## 📝 테스트 체크리스트

### 정상 시나리오 (✅ 완료 시 체크)

#### 인증
- [ ] A-1: 회원가입
- [ ] A-2: 로그인
- [ ] A-3: 토큰 갱신
- [ ] A-4: 로그아웃
- [ ] A-5: 비밀번호 변경

#### 매거진
- [ ] M-1: AI 매거진 생성
- [ ] M-2: 내 매거진 목록 조회
- [ ] M-3: 매거진 상세 조회
- [ ] M-4: 매거진 수정
- [ ] M-5: 매거진 삭제
- [ ] M-6: 매거진 공개 설정
- [ ] M-7: 공유 링크로 조회
- [ ] M-8: 키워드 검색
- [ ] M-9: 좋아요 토글
- [ ] M-10: 개인화 피드 조회

#### 사용자
- [ ] U-1: 내 프로필 조회
- [ ] U-2: 프로필 수정
- [ ] U-3: 다른 사용자 팔로우
- [ ] U-4: 언팔로우
- [ ] U-5: 팔로워 목록 조회
- [ ] U-6: 팔로잉 목록 조회

#### 무드보드
- [ ] B-1: 무드보드 생성

### 에러 시나리오 (✅ 완료 시 체크)

#### 인증 오류
- [ ] E-A1: 중복 아이디 회원가입
- [ ] E-A2: 중복 이메일 회원가입
- [ ] E-A3: 잘못된 비밀번호 로그인
- [ ] E-A4: 존재하지 않는 사용자 로그인
- [ ] E-A5: 만료된 refreshToken
- [ ] E-A6: 잘못된 현재 비밀번호로 변경
- [ ] E-A7: 인증 없이 보호된 API 호출

#### 매거진 오류
- [ ] E-M1: 존재하지 않는 매거진 조회
- [ ] E-M2: 다른 사용자 매거진 수정 시도
- [ ] E-M3: 다른 사용자 매거진 삭제 시도
- [ ] E-M4: 비공개 매거진 공유링크 접근
- [ ] E-M5: 유효하지 않은 shareToken
- [ ] E-M6: 빈 검색어로 검색
- [ ] E-M7: 수정 시 제목/소개 둘 다 비어있음

#### 사용자 오류
- [ ] E-U1: 자기 자신 팔로우
- [ ] E-U2: 이미 팔로우 중인 사용자 재팔로우
- [ ] E-U3: 팔로우 안 한 사용자 언팔로우
- [ ] E-U4: 존재하지 않는 사용자 팔로우
- [ ] E-U5: 중복 이메일로 프로필 수정

#### 경계값/특수 케이스
- [ ] E-X1: 매우 긴 제목 (100자 초과)
- [ ] E-X2: 매우 긴 소개 (500자 초과)
- [ ] E-X3: SQL Injection 시도
- [ ] E-X4: XSS 시도
- [ ] E-X5: 동시 좋아요 (동시성)
- [ ] E-X6: 음수 page/size

---

## 💡 추가 팁

### 효율적인 테스트 방법

1. **자동화 먼저**: `qa_test_runner.ps1`로 전체 테스트 실행
2. **실패 케이스 확인**: 결과 파일에서 FAIL 항목 확인
3. **수동 재현**: Swagger UI로 실패한 케이스 재현
4. **상세 로그 확인**: 서버 콘솔에서 에러 로그 확인
5. **Issue 등록**: 버그 확인 시 즉시 GitHub Issue 등록

### 테스트 데이터 관리

- 각 테스트 실행마다 고유한 타임스탬프 사용
- 테스트 사용자명: `testuser_TIMESTAMP`
- 테스트 이메일: `test_TIMESTAMP@example.com`
- 충돌 없이 반복 실행 가능

### 로그 확인

서버 콘솔에서 실시간 로그 확인:
```
# 서버 실행 시 로그 출력
./gradlew bootRun

# 에러 발생 시 스택 트레이스 확인
```

---

## 📞 문의

테스트 중 문제가 발생하거나 질문이 있으면:

- **GitHub Issues**: https://github.com/Jangjimin9766/Mine_server/issues
- **Label**: `question` 또는 `help wanted`

---

**Happy Testing! 🚀**
