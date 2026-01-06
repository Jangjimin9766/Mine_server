# Mine AI Server Migration Guide
> **대상**: Python FastAPI 서버 (`/Users/jangjimin/my_dev/Mine-AI`)
> **작성일**: 2026-01-06
> **작성자**: Spring 서버 담당 Antigravity

---

## 🚨 중요: 먼저 백업하세요!

```bash
cd /Users/jangjimin/my_dev
zip -r Mine-AI-backup-$(date +%Y%m%d-%H%M%S).zip Mine-AI -x "Mine-AI/.git/*" -x "Mine-AI/venv/*" -x "Mine-AI/__pycache__/*"
```

---

## 1. 현재 상황 요약

### 문제 발생
기존에 설계한 매거진 구조가 **실제 프로덕트 디자인과 맞지 않았습니다**.

### 기존 (잘못된) 이해
```
Magazine
├── Section 1 (문단 1)
├── Section 2 (문단 2)
├── Section 3 (문단 3)
└── 한 페이지에 쭉 나열
```

### 실제 프로덕트 구조
```
Magazine (큰 카테고리: 여행, 패션, 영화 등)
├── Moodboard (1개, 전체 분위기)
├── Section 1 (독립 주제: "여행가기 좋은 나라")  ← 그리드 카드
├── Section 2 (독립 주제: "한국 레저 명소")      ← 그리드 카드
├── Section 3 (독립 주제: "여행 장비")           ← 그리드 카드
└── 각 섹션 클릭 시 상세 본문 페이지
```

**핵심 차이점**:
- 섹션은 "문단"이 아니라 **독립된 콘텐츠 카드**
- 각 섹션은 고유한 주제와 충분한 본문을 가짐
- 그리드 형태로 배열됨

---

## 2. 다행인 점 (안심해도 됨)

| 항목 | 상태 |
|------|------|
| Spring ↔ Python API 통신 구조 | ✅ 동일 유지 |
| Section 데이터 필드 | ✅ 동일 (heading, content, imageUrl 등) |
| Moodboard 생성 로직 | ✅ 변경 없음 |
| DB 스키마 | ✅ 거의 동일 |

**변경되는 것**:
1. AI 프롬프트 (섹션 생성 방식)
2. 2단계 상호작용 (매거진/섹션 레벨 분리)
3. 본문 HTML 태그 생성

---

## 3. Spring 서버에서 변경되는 것

### 3.1 Interest 시스템
```
변경 전: Enum (10개)
변경 후: DB 테이블 (50개+)
```
- API 응답에서 Interest 목록 조회 가능해짐
- 매거진 생성 시 Interest 정보가 더 풍부해짐

### 3.2 Section 필드 추가
```java
// 기존
private String heading;
private String content;
private String imageUrl;
private String layoutType;

// 추가
private Integer displayOrder;  // 그리드 순서
```

### 3.3 API 엔드포인트 분리
```
기존:
POST /api/magazines/{magazineId}/interact

변경 후:
POST /api/magazines/{magazineId}/interact              ← 매거진 레벨
POST /api/magazines/{magazineId}/sections/{sectionId}/interact  ← 섹션 레벨
```

---

## 4. Python에서 구현해야 할 것

### 4.1 프롬프트 재설계 (필수)

**기존 프롬프트 (문단 생성)** ❌
```
"겨울 패션에 대한 매거진을 작성해줘. 
 여러 문단으로 나누어 서론, 본론, 결론 형태로..."
```

**새 프롬프트 (독립 주제 카드)** ✅
```
"'겨울 패션' 매거진에 들어갈 독립적인 콘텐츠 카드들을 생성해줘.

각 카드(섹션)는:
- 독립된 주제 (예: "올겨울 코트 트렌드", "액세서리 스타일링", "방한용품 추천")
- 제목 (heading): 해당 주제를 나타내는 매력적인 제목
- 본문 (content): 500-1500자의 완결된 내용
- 이미지 스타일 설명: Stable Diffusion 프롬프트용

섹션끼리는 연결될 필요 없음. 각자 독립적인 읽을거리."
```

### 4.2 HTML 태그 생성 (필수)

섹션의 `content`에 리치 텍스트 태그를 포함해서 생성:

```html
<h2>소제목</h2>
<p>일반 문단 내용입니다. 여러 줄에 걸쳐 작성될 수 있습니다.</p>
<blockquote>인용문이나 강조하고 싶은 내용</blockquote>
<p><strong>중요한 키워드</strong>는 이렇게 강조합니다.</p>
<ul>
  <li>리스트 항목 1</li>
  <li>리스트 항목 2</li>
</ul>
```

**AI 프롬프트에 추가**:
```
본문은 HTML 태그를 사용하여 구조화해줘:
- <p>: 일반 문단
- <h2>: 소제목
- <blockquote>: 인용/강조
- <strong>: 중요 단어
- <ul><li>: 리스트
```

### 4.3 2단계 상호작용 (핵심 변경)

#### 매거진 레벨 상호작용
**사용자 위치**: 매거진 페이지 (섹션들이 그리드로 보이는 곳)
**프롬프트 예시**: 
- "여행 장비에 대한 섹션 하나 추가해줘"
- "세 번째 섹션 삭제해줘"

**Python 처리**:
```python
# action: "create_section"
def handle_magazine_interact(message, magazine_data):
    if intent == "create_section":
        # 새 섹션 생성하여 반환
        return {
            "intent": "create_section",
            "success": True,
            "new_section": {
                "heading": "...",
                "content": "...",
                "image_url": "..."
            }
        }
    elif intent == "delete_section":
        # 삭제할 섹션 인덱스만 반환
        return {
            "intent": "delete_section",
            "success": True,
            "section_index": 2
        }
```

#### 섹션 레벨 상호작용
**사용자 위치**: 섹션 상세 페이지 (특정 섹션의 본문이 보이는 곳)
**프롬프트 예시**:
- "이 내용을 좀 더 감성적으로 바꿔줘"
- "분량을 늘려줘"
- "소제목을 추가해줘"

**Python 처리**:
```python
# action: "edit_section"  (새로운 action!)
def handle_section_interact(message, section_data):
    """
    특정 섹션의 본문만 수정
    """
    return {
        "intent": "regenerate_content",  # 또는 "edit_content"
        "success": True,
        "updated_section": {
            "heading": "...",
            "content": "<p>수정된 내용...</p>",
            "image_url": "..."
        }
    }
```

### 4.4 새 API 엔드포인트/핸들러 필요

**기존** (RunPod handler):
```python
if action == "edit_magazine":
    # 매거진 전체 수정
```

**추가**:
```python
if action == "edit_magazine":
    # 매거진 레벨 (섹션 생성/삭제)
    
elif action == "edit_section":  # 새로 추가!
    # 섹션 레벨 (본문 수정)
    section_id = data.get("section_id")
    section_data = data.get("section_data")
    message = data.get("message")
    # ... 해당 섹션만 수정
```

---

## 5. 변경 요약 체크리스트

### Python 필수 작업
- [ ] 프로젝트 백업 (zip)
- [ ] 섹션 생성 프롬프트 재설계 (문단 → 독립 카드)
- [ ] HTML 태그 생성 프롬프트 추가
- [ ] `edit_section` action 핸들러 추가
- [ ] 매거진 레벨 intent 분리: `create_section`, `delete_section`
- [ ] 섹션 레벨 intent 추가: `edit_content`, `regenerate_content`

### Python에서 안 바꿔도 되는 것
- [x] Spring 통신 구조 (동일)
- [x] 무드보드 생성 (동일)
- [x] 기존 섹션 필드 구조 (동일)
- [x] RunPod 배포 설정 (동일)

---

## 6. Spring ↔ Python 통신 형태

### 매거진 레벨 요청 (Spring → Python)
```json
{
  "action": "edit_magazine",
  "data": {
    "magazine_id": 1,
    "magazine_data": {
      "id": 1,
      "title": "여행 매거진",
      "sections": [...]
    },
    "message": "여행 장비에 대한 섹션 추가해줘"
  }
}
```

### 섹션 레벨 요청 (Spring → Python) - 신규!
```json
{
  "action": "edit_section",
  "data": {
    "magazine_id": 1,
    "section_id": 101,
    "section_data": {
      "heading": "여행가기 좋은 나라",
      "content": "<p>기존 본문...</p>"
    },
    "message": "이 내용 좀 더 감성적으로 바꿔줘"
  }
}
```

### Python 응답 (동일 구조 유지)
```json
{
  "intent": "regenerate_content",
  "success": true,
  "updated_section": {
    "heading": "여행가기 좋은 나라",
    "content": "<p>감성적으로 수정된 내용...</p>",
    "image_url": "..."
  }
}
```

---

## 7. 테스트 시나리오

### 시나리오 1: 섹션 생성
1. 사용자가 매거진 페이지에서 "여행 장비 섹션 추가해줘" 입력
2. Spring이 `edit_magazine` action으로 Python 호출
3. Python이 새 섹션 생성하여 반환
4. Spring이 DB에 저장

### 시나리오 2: 섹션 본문 수정
1. 사용자가 섹션 상세 페이지에서 "좀 더 재미있게 바꿔줘" 입력
2. Spring이 `edit_section` action으로 Python 호출
3. Python이 해당 섹션만 수정하여 반환
4. Spring이 DB 업데이트

---

## 8. 질문이 있다면

Spring 서버 관련 질문은 `/Users/jangjimin/my_dev/Mine-server` 프로젝트의 Antigravity에게 문의하세요.

핵심 참고 파일:
- `FASTAPI_GUIDE.md` - 기존 통신 가이드
- `MagazineInteractionService.java` - 상호작용 처리
- `InteractionDto.java` - 요청/응답 DTO
