# M:ine API QA Test - Normal Scenarios
# Normal Scenario Tests (A-1 ~ B-1)

$BASE_URL = "http://localhost:8080/api"
$TIMESTAMP = Get-Date -Format "yyyyMMddHHmmss"
$TEST_USERNAME = "testuser_$TIMESTAMP"
$TEST_EMAIL = "test_$TIMESTAMP@example.com"
$TEST_PASSWORD = "Test1234!@"
$TEST_NICKNAME = "Tester_$TIMESTAMP"

# Result Storage
$results = @()
$passCount = 0
$failCount = 0

function Test-API {
    param(
        [string]$TestId,
        [string]$Description,
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "[$TestId] $Description" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Method: $Method $Endpoint"
    
    try {
        $params = @{
            Uri         = "$BASE_URL$Endpoint"
            Method      = $Method
            Headers     = $Headers
            ContentType = "application/json"
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
            Write-Host "Request Body: $($params.Body)"
        }
        
        $response = Invoke-WebRequest @params -ErrorAction Stop
        $statusCode = $response.StatusCode
        $content = $response.Content | ConvertFrom-Json
        
        Write-Host "Status: $statusCode" -ForegroundColor Green
        Write-Host "Response: $($response.Content)"
        
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "✅ PASS" -ForegroundColor Green
            $script:passCount++
            $script:results += [PSCustomObject]@{
                TestId      = $TestId
                Description = $Description
                Status      = "PASS"
                StatusCode  = $statusCode
                Message     = "Success"
            }
            return $content
        }
        else {
            Write-Host "❌ FAIL - Expected $ExpectedStatus, got $statusCode" -ForegroundColor Red
            $script:failCount++
            $script:results += [PSCustomObject]@{
                TestId      = $TestId
                Description = $Description
                Status      = "FAIL"
                StatusCode  = $statusCode
                Message     = "Status code mismatch"
            }
            return $null
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status: $statusCode" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "❌ FAIL" -ForegroundColor Red
        $script:failCount++
        $script:results += [PSCustomObject]@{
            TestId      = $TestId
            Description = $Description
            Status      = "FAIL"
            StatusCode  = $statusCode
            Message     = $_.Exception.Message
        }
        return $null
    }
}

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "M:ine API QA Test - Normal Scenarios" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "Test User: $TEST_USERNAME"
Write-Host "Test Email: $TEST_EMAIL"
Write-Host ""

# ========================================
# 1. Authentication Tests
# ========================================

Write-Host "`n### 1. Authentication Tests ###`n" -ForegroundColor Yellow

# A-1: Signup
$signupBody = @{
    username = $TEST_USERNAME
    email    = $TEST_EMAIL
    password = $TEST_PASSWORD
    nickname = $TEST_NICKNAME
}
$userId = Test-API -TestId "A-1" -Description "Signup (Normal Data)" `
    -Method "POST" -Endpoint "/auth/signup" -Body $signupBody

Start-Sleep -Seconds 1

# A-2: Login
$loginBody = @{
    username = $TEST_USERNAME
    password = $TEST_PASSWORD
}
$loginResponse = Test-API -TestId "A-2" -Description "Login (Correct Credentials)" `
    -Method "POST" -Endpoint "/auth/login" -Body $loginBody

$accessToken = $loginResponse.accessToken
$refreshToken = $loginResponse.refreshToken

if ($accessToken) {
    Write-Host "`nAccess Token: $accessToken" -ForegroundColor Green
    Write-Host "Refresh Token: $refreshToken" -ForegroundColor Green
}

$authHeaders = @{
    "Authorization" = "Bearer $accessToken"
}

Start-Sleep -Seconds 1

# A-3: Token Refresh
if ($refreshToken) {
    $refreshBody = @{
        refreshToken = $refreshToken
    }
    $refreshResponse = Test-API -TestId "A-3" -Description "Token Refresh (Valid refreshToken)" `
        -Method "POST" -Endpoint "/auth/refresh" -Body $refreshBody
    
    if ($refreshResponse.accessToken) {
        $accessToken = $refreshResponse.accessToken
        $authHeaders = @{
            "Authorization" = "Bearer $accessToken"
        }
    }
}

Start-Sleep -Seconds 1

# A-5: Change Password
$passwordChangeBody = @{
    currentPassword = $TEST_PASSWORD
    newPassword     = "NewPassword123!@"
}
Test-API -TestId "A-5" -Description "Change Password (Correct Current Password)" `
    -Method "PATCH" -Endpoint "/auth/password" -Headers $authHeaders -Body $passwordChangeBody

# Login again with new password
$loginBody.password = "NewPassword123!@"
$loginResponse = Test-API -TestId "A-2-2" -Description "Login with New Password" `
    -Method "POST" -Endpoint "/auth/login" -Body $loginBody

$accessToken = $loginResponse.accessToken
$refreshToken = $loginResponse.refreshToken
$authHeaders = @{
    "Authorization" = "Bearer $accessToken"
}

Start-Sleep -Seconds 1

# ========================================
# 2. Magazine Tests
# ========================================

Write-Host "`n### 2. Magazine Tests ###`n" -ForegroundColor Yellow

# M-1: AI Magazine Creation (Internal API)
$magazineBody = @{
    title           = "Test Magazine $TIMESTAMP"
    subtitle        = "QA Test Subtitle"
    introduction    = "This is a magazine for QA testing."
    cover_image_url = "https://example.com/cover.jpg"
    user_email      = $TEST_EMAIL
    tags            = @("Test", "QA", "Automation")
    sections        = @(
        @{
            heading     = "Section 1"
            content     = "This is the content of section 1."
            image_url   = "https://example.com/section1.jpg"
            layout_type = "hero"
            caption     = "Section 1 Image"
        }
    )
}

$internalHeaders = @{
    "X-Internal-Key" = "mine-secret-key-1234"
}

$magazineResponse = Test-API -TestId "M-1" -Description "Create AI Magazine" `
    -Method "POST" -Endpoint "/internal/magazine" -Headers $internalHeaders -Body $magazineBody

$magazineId = $magazineResponse.magazineId

Start-Sleep -Seconds 1

# M-2: Get My Magazines
Test-API -TestId "M-2" -Description "Get My Magazines" `
    -Method "GET" -Endpoint "/magazines/my?page=0&size=10" -Headers $authHeaders

Start-Sleep -Seconds 1

# M-3: Get Magazine Detail
if ($magazineId) {
    $detailResponse = Test-API -TestId "M-3" -Description "Get Magazine Detail" `
        -Method "GET" -Endpoint "/magazines/$magazineId" -Headers $authHeaders
    
    if ($detailResponse -and $detailResponse.sections) {
        $sectionContent = $detailResponse.sections[0].content
        if ($sectionContent) {
            Write-Host "✅ Sections content verified: $sectionContent" -ForegroundColor Green
        }
        else {
            Write-Host "❌ FAIL - Sections content is missing or empty" -ForegroundColor Red
        }
    }
    else {
        Write-Host "❌ FAIL - Sections array is missing" -ForegroundColor Red
    }
}

Start-Sleep -Seconds 1

# M-4: Update Magazine
if ($magazineId) {
    $updateBody = @{
        title        = "Updated Magazine Title"
        introduction = "Updated introduction text."
    }
    Test-API -TestId "M-4" -Description "Update Magazine" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -Body $updateBody
}

Start-Sleep -Seconds 1

# M-6: Set Visibility
if ($magazineId) {
    $visibilityBody = @{
        isPublic = $true
    }
    $visibilityResponse = Test-API -TestId "M-6" -Description "Set Visibility" `
        -Method "POST" -Endpoint "/magazines/$magazineId/visibility" -Headers $authHeaders -Body $visibilityBody
    
    $shareToken = $visibilityResponse.shareToken
}

Start-Sleep -Seconds 1

# M-7: Get by Share Token
if ($shareToken) {
    Test-API -TestId "M-7" -Description "Get by Share Token" `
        -Method "GET" -Endpoint "/magazines/share/$shareToken"
}

Start-Sleep -Seconds 1

# M-8: Search
Test-API -TestId "M-8" -Description "Search Magazine" `
    -Method "GET" -Endpoint "/magazines/search?keyword=Test&page=0&size=10"

Start-Sleep -Seconds 1

# M-9: Toggle Like
if ($magazineId) {
    Test-API -TestId "M-9" -Description "Toggle Like" `
        -Method "POST" -Endpoint "/magazines/$magazineId/like" -Headers $authHeaders
}

Start-Sleep -Seconds 1

# M-10: Get Personalized Feed
Test-API -TestId "M-10" -Description "Get Personalized Feed" `
    -Method "GET" -Endpoint "/magazines/feed?page=0&size=10" -Headers $authHeaders

Start-Sleep -Seconds 1

# ========================================
# 3. User Tests
# ========================================

Write-Host "`n### 3. User Tests ###`n" -ForegroundColor Yellow

# Create second user for testing
$user2Username = "testuser2_$TIMESTAMP"
$user2Email = "test2_$TIMESTAMP@example.com"
$user2Body = @{
    username = $user2Username
    email    = $user2Email
    password = $TEST_PASSWORD
    nickname = "Tester2_$TIMESTAMP"
}
$user2Id = Test-API -TestId "Setup" -Description "Create Second User" `
    -Method "POST" -Endpoint "/auth/signup" -Body $user2Body

Start-Sleep -Seconds 1

# U-1: Get My Profile
Test-API -TestId "U-1" -Description "Get My Profile" `
    -Method "GET" -Endpoint "/users/me" -Headers $authHeaders

Start-Sleep -Seconds 1

# U-2: Update Profile
$profileUpdateBody = @{
    nickname = "UpdatedNick_$TIMESTAMP"
    bio      = "Updated bio text."
}
Test-API -TestId "U-2" -Description "Update Profile" `
    -Method "PATCH" -Endpoint "/users/me" -Headers $authHeaders -Body $profileUpdateBody

Start-Sleep -Seconds 1

# U-3: Follow User
if ($user2Id) {
    Test-API -TestId "U-3" -Description "Follow User" `
        -Method "POST" -Endpoint "/users/$user2Id/follow" -Headers $authHeaders
}

Start-Sleep -Seconds 1

# U-5: Get Followers
Test-API -TestId "U-5" -Description "Get Followers" `
    -Method "GET" -Endpoint "/users/$userId/followers?page=0&size=10" -Headers $authHeaders

Start-Sleep -Seconds 1

# U-6: Get Following
Test-API -TestId "U-6" -Description "Get Following" `
    -Method "GET" -Endpoint "/users/$userId/following?page=0&size=10" -Headers $authHeaders

Start-Sleep -Seconds 1

# U-4: Unfollow User
if ($user2Id) {
    Test-API -TestId "U-4" -Description "Unfollow User" `
        -Method "DELETE" -Endpoint "/users/$user2Id/follow" -Headers $authHeaders
}

Start-Sleep -Seconds 1

# ========================================
# 4. Moodboard Tests
# ========================================

Write-Host "`n### 4. Moodboard Tests ###`n" -ForegroundColor Yellow

# B-1: Create Moodboard (Requires Python Server)
$moodboardBody = @{
    prompt = "peaceful nature landscape with mountains"
    style  = "realistic"
}
Test-API -TestId "B-1" -Description "Create Moodboard" `
    -Method "POST" -Endpoint "/moodboards" -Headers $authHeaders -Body $moodboardBody

Start-Sleep -Seconds 1

# M-5: Delete Magazine (Execute Last)
if ($magazineId) {
    Test-API -TestId "M-5" -Description "Delete Magazine" `
        -Method "DELETE" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -ExpectedStatus 204
}

Start-Sleep -Seconds 1

# A-4: Logout (Execute Last)
Test-API -TestId "A-4" -Description "Logout" `
    -Method "POST" -Endpoint "/auth/logout" -Headers $authHeaders

# ========================================
# Test Results Summary
# ========================================

Write-Host "`n`n============================================" -ForegroundColor Yellow
Write-Host "Test Results Summary" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "Total Tests: $($passCount + $failCount)"
Write-Host "PASS: $passCount" -ForegroundColor Green
Write-Host "FAIL: $failCount" -ForegroundColor Red
Write-Host ""

# List invalid tests
if ($failCount -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $results | Where-Object { $_.Status -eq "FAIL" } | Format-Table -AutoSize
}

# Save results to file
$resultFile = "test_results_normal_$TIMESTAMP.json"
$results | ConvertTo-Json -Depth 10 | Out-File $resultFile
Write-Host "`nResults saved to: $resultFile" -ForegroundColor Cyan
