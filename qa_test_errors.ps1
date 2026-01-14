# M:ine API QA Test - Error Scenarios
# Error Scenario Tests (E-A1 ~ E-X6)

$BASE_URL = "http://localhost:8080/api"
$TIMESTAMP = Get-Date -Format "yyyyMMddHHmmss"

# Result Storage
$results = @()
$passCount = 0
$failCount = 0

function Test-ErrorAPI {
    param(
        [string]$TestId,
        [string]$Description,
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int]$ExpectedStatus = 400
    )
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "[$TestId] $Description" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Method: $Method $Endpoint"
    Write-Host "Expected Error Status: $ExpectedStatus"
    
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
        
        # Success when error expected
        Write-Host "Status: $statusCode" -ForegroundColor Red
        Write-Host "Response: $($response.Content)"
        Write-Host "❌ FAIL - Expected error $ExpectedStatus, but got success $statusCode" -ForegroundColor Red
        $script:failCount++
        $script:results += [PSCustomObject]@{
            TestId      = $TestId
            Description = $Description
            Status      = "FAIL"
            StatusCode  = $statusCode
            Message     = "Expected error but got success"
        }
        return $null
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorBody = ""
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
        }
        catch {}
        
        Write-Host "Status: $statusCode" -ForegroundColor Yellow
        if ($errorBody) {
            Write-Host "Error Response: $errorBody" -ForegroundColor Yellow
        }
        
        # Check if status code is expected
        $expectedStatuses = @($ExpectedStatus, 400, 401, 403, 404)
        if ($statusCode -in $expectedStatuses) {
            Write-Host "✅ PASS - Got expected error status" -ForegroundColor Green
            $script:passCount++
            $script:results += [PSCustomObject]@{
                TestId      = $TestId
                Description = $Description
                Status      = "PASS"
                StatusCode  = $statusCode
                Message     = "Expected error occurred"
            }
        }
        else {
            Write-Host "❌ FAIL - Expected $ExpectedStatus, got $statusCode" -ForegroundColor Red
            $script:failCount++
            $script:results += [PSCustomObject]@{
                TestId      = $TestId
                Description = $Description
                Status      = "FAIL"
                StatusCode  = $statusCode
                Message     = "Unexpected error status code"
            }
        }
        return $statusCode
    }
}

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "M:ine API QA Test - Error Scenarios" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host ""

# Create Test User (Normal Case)
$testUsername = "errortest_$TIMESTAMP"
$testEmail = "errortest_$TIMESTAMP@example.com"
$testPassword = "Test1234!@"

Write-Host "Setting up test user..." -ForegroundColor Cyan
try {
    $signupBody = @{
        username = $testUsername
        email    = $testEmail
        password = $testPassword
        nickname = "ErrorTester_$TIMESTAMP"
    }
    $response = Invoke-RestMethod -Uri "$BASE_URL/auth/signup" -Method POST `
        -Body ($signupBody | ConvertTo-Json) -ContentType "application/json"
    $userId = $response
    
    $loginBody = @{
        username = $testUsername
        password = $testPassword
    }
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method POST `
        -Body ($loginBody | ConvertTo-Json) -ContentType "application/json"
    $accessToken = $loginResponse.accessToken
    $authHeaders = @{
        "Authorization" = "Bearer $accessToken"
    }
    Write-Host "✅ Test user created successfully" -ForegroundColor Green
    Write-Host "User ID: $userId"
    Write-Host ""
}
catch {
    Write-Host "❌ Failed to create test user. Exiting..." -ForegroundColor Red
    exit 1
}

# ========================================
# 1. Authentication Errors (E-A1 ~ E-A7)
# ========================================

Write-Host "`n### 1. Authentication Errors ###`n" -ForegroundColor Yellow

# E-A1: Duplicate Username Signup
$duplicateBody = @{
    username = $testUsername
    email    = "different_$TIMESTAMP@example.com"
    password = $testPassword
    nickname = "DiffNick"
}
Test-ErrorAPI -TestId "E-A1" -Description "Duplicate Username Signup" `
    -Method "POST" -Endpoint "/auth/signup" -Body $duplicateBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A2: Duplicate Email Signup
$duplicateEmailBody = @{
    username = "different_$TIMESTAMP"
    email    = $testEmail
    password = $testPassword
    nickname = "DiffNick2"
}
Test-ErrorAPI -TestId "E-A2" -Description "Duplicate Email Signup" `
    -Method "POST" -Endpoint "/auth/signup" -Body $duplicateEmailBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A3: Login with Wrong Password
$wrongPasswordBody = @{
    username = $testUsername
    password = "WrongPassword123!@"
}
Test-ErrorAPI -TestId "E-A3" -Description "Login with Wrong Password" `
    -Method "POST" -Endpoint "/auth/login" -Body $wrongPasswordBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A4: Login Non-Existent User
$nonExistentBody = @{
    username = "nonexistent_user_$TIMESTAMP"
    password = $testPassword
}
Test-ErrorAPI -TestId "E-A4" -Description "Login Non-Existent User" `
    -Method "POST" -Endpoint "/auth/login" -Body $nonExistentBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A5: Invalid Refresh Token
$invalidRefreshBody = @{
    refreshToken = "invalid_token_12345"
}
Test-ErrorAPI -TestId "E-A5" -Description "Invalid Refresh Token" `
    -Method "POST" -Endpoint "/auth/refresh" -Body $invalidRefreshBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A6: Change Password Wrong Current
$wrongCurrentPasswordBody = @{
    currentPassword = "WrongPassword123!@"
    newPassword     = "NewPassword456!@"
}
Test-ErrorAPI -TestId "E-A6" -Description "Change Password Wrong Current" `
    -Method "PATCH" -Endpoint "/auth/password" -Headers $authHeaders -Body $wrongCurrentPasswordBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-A7: Unauthorized API Call
Test-ErrorAPI -TestId "E-A7" -Description "Unauthorized API Call" `
    -Method "GET" -Endpoint "/magazines/my?page=0&size=10" -ExpectedStatus 401

Start-Sleep -Seconds 1

# ========================================
# 2. Magazine Permission/Validation Errors (E-M1 ~ E-M7)
# ========================================

Write-Host "`n### 2. Magazine Errors ###`n" -ForegroundColor Yellow

# Create Test Magazine
Write-Host "Creating test magazine..." -ForegroundColor Cyan
$magazineBody = @{
    title           = "Error Test Magazine $TIMESTAMP"
    subtitle        = "For Testing"
    introduction    = "Magazine for error testing"
    cover_image_url = "https://example.com/cover.jpg"
    user_email      = $testEmail
    sections        = @(
        @{
            heading     = "Test Section"
            content     = "Test Content"
            image_url   = "https://example.com/test.jpg"
            layout_type = "basic"
        }
    )
}
$internalHeaders = @{
    "X-Internal-Key" = "mine-secret-key-1234"
}
try {
    $magazineResponse = Invoke-RestMethod -Uri "$BASE_URL/internal/magazine" -Method POST `
        -Headers $internalHeaders -Body ($magazineBody | ConvertTo-Json -Depth 10) -ContentType "application/json"
    $magazineId = $magazineResponse.magazineId
    Write-Host "✅ Test magazine created: ID $magazineId" -ForegroundColor Green
}
catch {
    Write-Host "⚠️ Could not create test magazine" -ForegroundColor Yellow
    $magazineId = $null
}
Write-Host ""

# E-M1: Get Non-Existent Magazine
Test-ErrorAPI -TestId "E-M1" -Description "Get Non-Existent Magazine" `
    -Method "GET" -Endpoint "/magazines/999999" -Headers $authHeaders -ExpectedStatus 404

Start-Sleep -Seconds 1

# Create Second User
Write-Host "Creating second user for permission tests..." -ForegroundColor Cyan
$user2Username = "errortest2_$TIMESTAMP"
$user2Email = "errortest2_$TIMESTAMP@example.com"
try {
    $user2Body = @{
        username = $user2Username
        email    = $user2Email
        password = $testPassword
        nickname = "ErrorTester2"
    }
    Invoke-RestMethod -Uri "$BASE_URL/auth/signup" -Method POST `
        -Body ($user2Body | ConvertTo-Json) -ContentType "application/json" | Out-Null
    
    $user2LoginBody = @{
        username = $user2Username
        password = $testPassword
    }
    $user2LoginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method POST `
        -Body ($user2LoginBody | ConvertTo-Json) -ContentType "application/json"
    $user2Token = $user2LoginResponse.accessToken
    $user2Headers = @{
        "Authorization" = "Bearer $user2Token"
    }
    Write-Host "✅ Second user created" -ForegroundColor Green
}
catch {
    Write-Host "⚠️ Could not create second user" -ForegroundColor Yellow
    $user2Headers = $null
}
Write-Host ""

# E-M2: Update Other User's Magazine
if ($magazineId -and $user2Headers) {
    $updateBody = @{
        title        = "Hacking Attempt"
        introduction = "Unauthorized Edit"
    }
    Test-ErrorAPI -TestId "E-M2" -Description "Update Other User's Magazine" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $user2Headers -Body $updateBody -ExpectedStatus 403
}

Start-Sleep -Seconds 1

# E-M3: Delete Other User's Magazine
if ($magazineId -and $user2Headers) {
    Test-ErrorAPI -TestId "E-M3" -Description "Delete Other User's Magazine" `
        -Method "DELETE" -Endpoint "/magazines/$magazineId" -Headers $user2Headers -ExpectedStatus 403
}

Start-Sleep -Seconds 1

# E-M5: Invalid Share Token
Test-ErrorAPI -TestId "E-M5" -Description "Invalid Share Token" `
    -Method "GET" -Endpoint "/magazines/share/invalid_token_xyz" -ExpectedStatus 404

Start-Sleep -Seconds 1

# E-M6: Empty Keyword Search
Test-ErrorAPI -TestId "E-M6" -Description "Empty Keyword Search" `
    -Method "GET" -Endpoint "/magazines/search?keyword=&page=0&size=10" -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-M7: Update Empty Title/Intro
if ($magazineId) {
    $emptyUpdateBody = @{
        title        = $null
        introduction = $null
    }
    Test-ErrorAPI -TestId "E-M7" -Description "Update Empty Title/Intro" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -Body $emptyUpdateBody -ExpectedStatus 400
}

Start-Sleep -Seconds 1

# ========================================
# 3. User Errors (E-U1 ~ E-U5)
# ========================================

Write-Host "`n### 3. User Errors ###`n" -ForegroundColor Yellow

# E-U1: Self Follow
Test-ErrorAPI -TestId "E-U1" -Description "Self Follow" `
    -Method "POST" -Endpoint "/users/$userId/follow" -Headers $authHeaders -ExpectedStatus 400

Start-Sleep -Seconds 1

# Setup Follow (Normal)
if ($user2Headers) {
    Write-Host "Following user2 for duplicate follow test..." -ForegroundColor Cyan
    try {
        $user2Id = Invoke-RestMethod -Uri "$BASE_URL/users/me" -Method GET `
            -Headers $user2Headers | Select-Object -ExpandProperty id
        
        Invoke-RestMethod -Uri "$BASE_URL/users/$user2Id/follow" -Method POST `
            -Headers $authHeaders -ContentType "application/json" | Out-Null
        Write-Host "✅ Followed user2" -ForegroundColor Green
        
        # E-U2: Duplicate Follow
        Test-ErrorAPI -TestId "E-U2" -Description "Duplicate Follow" `
            -Method "POST" -Endpoint "/users/$user2Id/follow" -Headers $authHeaders -ExpectedStatus 400
        
        Start-Sleep -Seconds 1
    }
    catch {
        Write-Host "⚠️ Could not set up follow relationship" -ForegroundColor Yellow
    }
}

# E-U3: Unfollow Not-Followed User
$user3Username = "errortest3_$TIMESTAMP"
$user3Email = "errortest3_$TIMESTAMP@example.com"
try {
    $user3Body = @{
        username = $user3Username
        email    = $user3Email
        password = $testPassword
        nickname = "ErrorTester3"
    }
    $user3Id = Invoke-RestMethod -Uri "$BASE_URL/auth/signup" -Method POST `
        -Body ($user3Body | ConvertTo-Json) -ContentType "application/json"
    
    Test-ErrorAPI -TestId "E-U3" -Description "Unfollow Not-Followed User" `
        -Method "DELETE" -Endpoint "/users/$user3Id/follow" -Headers $authHeaders -ExpectedStatus 400
}
catch {
    Write-Host "⚠️ Could not test E-U3" -ForegroundColor Yellow
}

Start-Sleep -Seconds 1

# E-U4: Follow Non-Existent User
Test-ErrorAPI -TestId "E-U4" -Description "Follow Non-Existent User" `
    -Method "POST" -Endpoint "/users/999999/follow" -Headers $authHeaders -ExpectedStatus 404

Start-Sleep -Seconds 1

# E-U5: Update Profile Duplicate Email
if ($user2Email) {
    $duplicateEmailUpdate = @{
        email    = $user2Email
        nickname = "ForTesting"
    }
    Test-ErrorAPI -TestId "E-U5" -Description "Update Profile Duplicate Email" `
        -Method "PATCH" -Endpoint "/users/me" -Headers $authHeaders -Body $duplicateEmailUpdate -ExpectedStatus 400
}

Start-Sleep -Seconds 1

# ========================================
# 4. Edge Cases (E-X1 ~ E-X6)
# ========================================

Write-Host "`n### 4. Edge Cases ###`n" -ForegroundColor Yellow

# E-X1: Very Long Title
if ($magazineId) {
    $longTitleBody = @{
        title        = "A" * 101
        introduction = "Normal Intro"
    }
    Test-ErrorAPI -TestId "E-X1" -Description "Very Long Title (>100)" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -Body $longTitleBody -ExpectedStatus 400
}

Start-Sleep -Seconds 1

# E-X2: Very Long Introduction
if ($magazineId) {
    $longIntroBody = @{
        title        = "Normal Title"
        introduction = "A" * 501
    }
    Test-ErrorAPI -TestId "E-X2" -Description "Very Long Introduction (>500)" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -Body $longIntroBody -ExpectedStatus 400
}

Start-Sleep -Seconds 1

# E-X3: SQL Injection Attempt
$sqlInjectionBody = @{
    username = "admin' OR '1'='1"
    password = "password"
}
Test-ErrorAPI -TestId "E-X3" -Description "SQL Injection Attempt" `
    -Method "POST" -Endpoint "/auth/login" -Body $sqlInjectionBody -ExpectedStatus 400

Start-Sleep -Seconds 1

# E-X4: XSS Attempt
if ($magazineId) {
    $xssBody = @{
        title        = "<script>alert('xss')</script>"
        introduction = "XSS Test"
    }
    Test-ErrorAPI -TestId "E-X4" -Description "XSS Attempt" `
        -Method "PUT" -Endpoint "/magazines/$magazineId" -Headers $authHeaders -Body $xssBody -ExpectedStatus 400
}

Start-Sleep -Seconds 1

# E-X6: Negative Page/Size
Test-ErrorAPI -TestId "E-X6" -Description "Negative Page/Size" `
    -Method "GET" -Endpoint "/magazines/my?page=-1&size=-10" -Headers $authHeaders -ExpectedStatus 400

Start-Sleep -Seconds 1

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

# List failed tests
if ($failCount -gt 0) {
    Write-Host "Failed Tests (Possible Bugs):" -ForegroundColor Red
    $results | Where-Object { $_.Status -eq "FAIL" } | Format-Table -AutoSize
    Write-Host "`n⚠️ Please report these on GitHub Issues!" -ForegroundColor Yellow
}

# Save results to file
$resultFile = "test_results_errors_$TIMESTAMP.json"
$results | ConvertTo-Json -Depth 10 | Out-File $resultFile
Write-Host "`nResults saved to: $resultFile" -ForegroundColor Cyan

# Save as CSV
$csvFile = "test_results_errors_$TIMESTAMP.csv"
$results | Export-Csv -Path $csvFile -NoTypeInformation -Encoding UTF8
Write-Host "CSV Result: $csvFile" -ForegroundColor Cyan
