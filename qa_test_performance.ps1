# Performance Test Script
$BASE_URL = "http://127.0.0.1:8080/api/magazines/search"
$KEYWORD = "test"
$ITERATIONS = 5

Write-Host "Starting Performance Test (Search API)..." -ForegroundColor Cyan
Write-Host "Target: < 0.1s (100ms)" -ForegroundColor Gray

$totalTime = 0
$successCount = 0

for ($i = 1; $i -le $ITERATIONS; $i++) {
    $startTime = Get-Date
    
    try {
        $uri = "$BASE_URL?keyword=$KEYWORD&page=0&size=10"
        # Write-Host "Invoking: $uri"
        $response = Invoke-WebRequest -Uri $uri -Method Get -ErrorAction Stop
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalMilliseconds
        
        Write-Host "Request ${i}: $duration ms" -ForegroundColor Green
        
        $totalTime += $duration
        $successCount++
    }
    catch {
        Write-Host "Request ${i} Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($successCount -gt 0) {
    $avgTime = $totalTime / $successCount
    Write-Host "`nAverage Response Time: $avgTime ms" -ForegroundColor Yellow

    if ($avgTime -lt 100) {
        Write-Host "✅ PERFORMANCE GOAL MET (< 100ms)" -ForegroundColor Green
    }
    else {
        Write-Host "⚠️ PERFORMANCE GOAL NOT MET (> 100ms)" -ForegroundColor Red
    }
}
else {
    Write-Host "❌ ALL REQUESTS FAILED" -ForegroundColor Red
}
