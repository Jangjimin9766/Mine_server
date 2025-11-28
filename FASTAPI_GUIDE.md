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
  "introduction": "Short intro text...",
  "cover_image_url": "http://image.url/cover.jpg",
  "user_email": "user@example.com",  // [REQUIRED] Must match a registered user's email
  "sections": [
    {
      "heading": "Section 1 Title",
      "content": "Main content text...",
      "image_url": "http://image.url/sec1.jpg",
      "layout_hint": "image_left" // Options: "image_left", "full_width", etc.
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
@NoArgsConstructor
public class MagazineCreateRequest {
    private String title;
    private String introduction;
    
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    
    @JsonProperty("user_email")
    private String userEmail; // Maps to 'user_email' in JSON
    
    private List<SectionDto> sections;

    @Getter
    @NoArgsConstructor
    public static class SectionDto {
        private String heading;
        private String content;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        @JsonProperty("layout_hint")
        private String layoutHint;
    }
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
