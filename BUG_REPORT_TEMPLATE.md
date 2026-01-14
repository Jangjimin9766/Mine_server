# 🐛 버그 리포트

> **이 템플릿을 사용하여 GitHub Issue를 작성해주세요**  
> Repository: https://github.com/Jangjimin9766/Mine_server/issues

---

## 테스트 케이스 ID
> 예: E-M3, A-1, U-2

**[여기에 테스트 케이스 ID 입력]**

---

## 환경

- **OS**: (예: Windows 11, macOS Sonoma, Ubuntu 22.04)
- **테스트 도구**: (예: Swagger UI, PowerShell 자동화 스크립트, Postman, cURL)
- **브라우저** (Swagger 사용 시): (예: Chrome 120, Firefox 121)
- **서버 버전**: (예: commit hash 또는 branch 이름)

---

## 재현 방법

> 버그를 재현하기 위한 단계별 설명

1. 
2. 
3. 

**예시:**
```
1. 사용자 A로 로그인 (username: testuser1)
2. 사용자 B의 매거진 ID(123)를 확인
3. PUT /api/magazines/123 호출하여 제목 수정 시도
4. Authorization 헤더에 사용자 A의 토큰 사용
```

---

## 예상 결과

> 정상적으로 동작했을 때 예상되는 결과

**예시:**
```
403 Forbidden
{
  "error": "권한이 없습니다"
}
```

---

## 실제 결과

> 실제로 발생한 결과 (버그)

**예시:**
```
200 OK
{
  "id": 123,
  "title": "수정된 제목",
  ...
}
```

---

## 스크린샷/로그

> 가능하면 스크린샷이나 로그를 첨부해주세요

### 스크린샷
[여기에 스크린샷 첨부 또는 드래그 앤 드롭]

### 서버 로그
```
[여기에 관련 서버 로그 붙여넣기]
```

### 요청/응답 예시
```json
// Request
POST /api/auth/login
{
  "username": "testuser",
  "password": "wrongpassword"
}

// Response
200 OK
{
  "accessToken": "eyJ..."
}
```

---

## 심각도

> 버그의 심각도를 선택해주세요

- [ ] 🔴 Critical - 서비스 중단 또는 보안 취약점
- [ ] 🟠 High - 주요 기능 동작 불가
- [ ] 🟡 Medium - 일부 기능 오작동
- [ ] 🟢 Low - 사소한 문제 또는 개선 사항

---

## 추가 정보

> 버그와 관련된 추가 정보나 컨텍스트

**예시:**
- 이 버그는 권한 체크가 누락되어 발생하는 보안 취약점입니다
- 다른 사용자의 매거진을 수정/삭제할 수 있습니다
- MagazineController의 updateMagazine 메서드에서 권한 검증 필요

---

## 관련 테스트 케이스

> 이 버그와 관련된 다른 테스트 케이스가 있다면 나열

- E-M2: 다른 사용자 매거진 수정 시도
- E-M3: 다른 사용자 매거진 삭제 시도

---

## 제안 사항 (선택)

> 버그 수정을 위한 제안이 있다면 작성

**예시:**
```java
// MagazineController.updateMagazine()에 권한 체크 추가
if (!magazine.getUser().getUsername().equals(userDetails.getUsername())) {
    throw new ForbiddenException("권한이 없습니다");
}
```

---

## 체크리스트

작성 전 확인해주세요:

- [ ] 테스트 케이스 ID를 명확히 기재했습니다
- [ ] 재현 방법을 단계별로 작성했습니다
- [ ] 예상 결과와 실제 결과를 명확히 구분했습니다
- [ ] 가능하면 스크린샷이나 로그를 첨부했습니다
- [ ] 심각도를 선택했습니다
- [ ] 적절한 Label을 추가했습니다 (bug, security, enhancement 등)

---

**감사합니다! 🙏**
