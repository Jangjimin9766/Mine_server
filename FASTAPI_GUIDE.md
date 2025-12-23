# Spring Server Integration Guide for FastAPI

This document provides all necessary details for integrating the FastAPI server with the Spring Boot backend (`mine-server`).

## 1. System Overview
- **Spring Server**: Handles User Management (Auth), Database (MySQL), and Magazine Storage.
- **FastAPI Server**: Generates Magazine content using AI and sends it to Spring Server.

## 2. User Management Flow (Critical)
> [!IMPORTANT]
> **Before creating a magazine, the user MUST exist in the Spring Database.**
> The FastAPI server sends `user_email` when creating a magazine. If this email does not belong to a registered user, the request will fail.

### Recommended Flow
1.  **User Signup/Login**: Users should sign up/login via the Client App (interacting with Spring Server).
2.  **Token Issue**: Spring Server issues a JWT token.
3.  **AI Request**: The Client App sends a request to FastAPI, including the user's email (or token).
4.  **Magazine Creation**: FastAPI generates content and sends it to Spring Server with the `user_email`.

## 3. API Specifications

### 3.1 Create Magazine (Internal API)
Used by FastAPI to save generated magazines.

- **Endpoint**: `POST http://localhost:8080/api/internal/magazine`
- **Headers**:
    - `Content-Type`: `application/json`
    - *(No Authorization header needed for this internal endpoint)*

- **Request Body (JSON)**:
```json
{
  "title": "Magazine Title",
  "subtitle": "Magazine Subtitle",
  "introduction": "Short intro text...",
  "cover_image_url": "http://image.url/cover.jpg",
  "user_email": "user@example.com",
  "tags": ["fashion", "winter", "style"],
  "moodboard": {
    "image_url": "http://image.url/moodboard.jpg",
    "description": "A cozy winter atmosphere with warm tones"
  },
  "sections": [
    {
      "heading": "Section 1 Title",
      "content": "Main content text...",
      "image_url": "http://image.url/sec1.jpg",
      "layout_hint": "image_left",
      "layout_type": "hero",
      "caption": "2024 Winter Collection"
    }
  ]
}
```

- **Response**:
    - `200 OK`: Returns the created Magazine ID (Long).
    - `500 Internal Server Error`: If `user_email` is not found or other DB errors.

### 3.2 User Signup (For Testing)
If you need to create a test user manually:

- **Endpoint**: `POST http://localhost:8080/api/auth/signup`
- **Body**:
```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "Tester"
}
```

## 4. Data Structures (Spring DTOs)

### MagazineCreateRequest.java
```java
@Getter
@Setter
@NoArgsConstructor
public class MagazineCreateRequest {
    private String title;
    
    private String subtitle;  // [NEW] 부제
    
    private String introduction;
    
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    
    @JsonProperty("user_email")
    private String userEmail; // Maps to 'user_email' in JSON
    
    private List<String> tags;  // [NEW] 태그 목록
    
    private MoodboardResponseDto moodboard;  // [NEW] 무드보드 데이터
    
    private List<SectionDto> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SectionDto {
        private String heading;
        private String content;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        @JsonProperty("layout_hint")
        private String layoutHint;
        
        @JsonProperty("layout_type")  // [NEW] 'hero', 'quote', 'split_left', 'split_right', 'basic' 등
        private String layoutType;
        
        private String caption;  // [NEW] 이미지 캡션 (Optional)
    }
}
```

### MoodboardResponseDto.java (Nested in MagazineCreateRequest)
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodboardResponseDto {
    private String image_url;  // 무드보드 이미지 URL
    private String description;  // 무드보드 설명
}
```

## 5. Troubleshooting

### "User not found" Error
- **Symptom**: 500 Error or 403 Error when calling `/api/internal/magazine`.
- **Cause**: The `user_email` sent in the JSON does not exist in the `users` table.
- **Fix**: Ensure the user is signed up via `/api/auth/signup` before sending the magazine.

### 403 Forbidden
- **Cause**: Accessing a protected endpoint without a valid token, or accessing `/error` page when an exception occurs (fixed in latest update).
- **Fix**: Internal API `/api/internal/**` is open. If you get 403, check if the server logs show an exception (like "User not found") which might be redirecting to a protected error page.

## 6. Security & Production Setup (RunPod / Deployment)

### 6.1 Timeout Configuration
The AI generation process takes time (10-30s+). The Spring Boot server `ReadTimeout` has been increased to **90 seconds** in `AppConfig.java`.
Ensure your Python server does not have its own timeout disconnects (usually Uvicorn/Gunicorn defaults are fine, but check Reverse Proxy settings if using Nginx).

### 6.2 Connection Address
In `application.yml`, update the `python.api.url` to your actual RunPod URL:
```yaml
python:
  api:
    url: http://YOUR-RUNPOD-ID-8000.proxy.runpod.net/api/magazine/create
    # ...
```

### 6.3 API Key Authentication
To prevent unauthorized usage of your GPU resources, an API Key mechanism has been added.

**Spring Boot (Client) Changes:**
- `X-API-KEY` header is now sent with every request to the Python server.
- Key is configured in `application.yml` under `python.api.key`.

**FastAPI (Server) Required Changes:**
You must update your `main.py` (or equivalent) to verify this key.

**Recommended `main.py` Implementation:**
```python
from fastapi import FastAPI, Header, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
import os

app = FastAPI()

# --- Security Configuration ---
API_KEY = os.getenv("PYTHON_API_KEY", "mine-secret-key-1234")

async def verify_api_key(x_api_key: str = Header(...)):
    """Validates the API Key sent in the header"""
    if x_api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API Key")

# --- CORS Configuration ---
# Allow only trusted origins (e.g., your Spring Boot server)
origins = [
    "http://localhost:8080",
    "http://127.0.0.1:8080",
    # Add your production Spring Boot URL here if deployed
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins, # Restrict this instead of ["*"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Applying Security to Endpoints ---
# Option A: Apply to the specific router
# app.include_router(magazine.router, dependencies=[Depends(verify_api_key)])

# Option B: Apply to specific endpoint
@app.post("/api/magazine/create", dependencies=[Depends(verify_api_key)])
async def create_magazine(request: MagazineRequest):
    # Your logic here
    pass
```
