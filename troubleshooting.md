# 🛠️ M:ine Troubleshooting Log (CIJ Phase)

## 🚨 Issue 1: Authentication Failure (500 Error)
- **Problem**: Login and Logout APIs return 500 Internal Server Error.
- **Cause**: `RefreshToken` is configured to use Redis (`@RedisHash`), but the local Redis server (port 6379) is not running.
- **Solution**: 
    - Ensure Redis is started before running the application.
    - (Future) Consider a fallback to JPA or a mock repository for local-only development.

## 🚨 Issue 2: Magazine Generation Failure (400 Error)
- **Problem**: AI Magazine creation (`POST /api/magazines`) fails.
- **Cause**: The application attempts to connect to a Python AI server on `localhost:8000`, which is likely not running or the URL is incorrect for the current environment.
- **Solution**: 
    - Verify Python server status.
    - Check `application.yml` for the correct `python.api.url`.

## 🚨 Issue 3: Missing Body Content in Response
- **Problem**: Users requested magazine body content in API responses.
- **Cause**: The controller was returning the raw `Magazine` entity, which might have lazy-loading issues or lack necessary DTO mapping for optimized payload.
- **Action**: 
    - Update `MagazineController` to return `MagazineDto.Response`.
    - Enhance `qa_test_normal.ps1` to explicitly verify the `sections` array.
