# M:ine API QA Test Runner
# Executes all tests and generates a comprehensive report

param(
    [switch]$NormalOnly,
    [switch]$ErrorsOnly,
    [switch]$Quick
)

$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"

Write-Host "============================================" -ForegroundColor Magenta
Write-Host "M:ine API QA Test Runner" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta
Write-Host "Start Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host ""

# Check Server Status
Write-Host "Checking server connection..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/swagger-ui.html" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Server is running normally." -ForegroundColor Green
}
catch {
    Write-Host "❌ Cannot connect to server!" -ForegroundColor Red
    Write-Host "Please check if M:ine server is running at http://localhost:8080" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "How to run server:" -ForegroundColor Yellow
    Write-Host "  ./gradlew bootRun" -ForegroundColor White
    exit 1
}

Write-Host ""

# Execute Tests
$testResults = @{
    Normal = $null
    Errors = $null
}

if (-not $ErrorsOnly) {
    Write-Host "`n================================================" -ForegroundColor Yellow
    Write-Host "1. Running Normal Scenario Tests..." -ForegroundColor Yellow
    Write-Host "================================================" -ForegroundColor Yellow
    
    if (Test-Path ".\qa_test_normal.ps1") {
        & .\qa_test_normal.ps1
        $testResults.Normal = "Completed"
    }
    else {
        Write-Host "❌ qa_test_normal.ps1 not found." -ForegroundColor Red
        $testResults.Normal = "Skipped"
    }
    
    Write-Host "`nWaiting... (5 seconds)" -ForegroundColor Gray
    Start-Sleep -Seconds 5
}

if (-not $NormalOnly) {
    Write-Host "`n================================================" -ForegroundColor Yellow
    Write-Host "2. Running Error Scenario Tests..." -ForegroundColor Yellow
    Write-Host "================================================" -ForegroundColor Yellow
    
    if (Test-Path ".\qa_test_errors.ps1") {
        & .\qa_test_errors.ps1
        $testResults.Errors = "Completed"
    }
    else {
        Write-Host "❌ qa_test_errors.ps1 not found." -ForegroundColor Red
        $testResults.Errors = "Skipped"
    }
}

# Generate Comprehensive Report
Write-Host "`n`n================================================" -ForegroundColor Magenta
Write-Host "Test Execution Completed!" -ForegroundColor Magenta
Write-Host "================================================" -ForegroundColor Magenta
Write-Host "End Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host ""

Write-Host "Executed Tests:" -ForegroundColor Cyan
if ($testResults.Normal) {
    Write-Host "  ✅ Normal Scenarios: $($testResults.Normal)" -ForegroundColor Green
}
if ($testResults.Errors) {
    Write-Host "  ✅ Error Scenarios: $($testResults.Errors)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Generated Result Files:" -ForegroundColor Cyan
Get-ChildItem -Filter "test_results_*.json" | Sort-Object LastWriteTime -Descending | Select-Object -First 5 | ForEach-Object {
    Write-Host "  📄 $($_.Name)" -ForegroundColor White
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Check result files" -ForegroundColor White
Write-Host "  2. Report failed tests as GitHub Issues" -ForegroundColor White
Write-Host "  3. Use BUG_REPORT_TEMPLATE.md to report issues" -ForegroundColor White
Write-Host ""
Write-Host "GitHub Issues: https://github.com/Jangjimin9766/Mine_server/issues" -ForegroundColor Cyan
Write-Host ""
