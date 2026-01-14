#!/bin/bash
# M:ine API QA Test Runner (Bash version)
# Git Bash 및 Linux/macOS에서 실행 가능

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "============================================"
echo "M:ine API QA Test Runner"
echo "============================================"
echo "시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 서버 상태 확인
echo "서버 연결 확인 중..."
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui.html" | grep -q "200"; then
    echo "✅ 서버가 정상적으로 실행 중입니다."
else
    echo "❌ 서버에 연결할 수 없습니다!"
    echo "http://localhost:8080 에서 M:ine 서버가 실행 중인지 확인해주세요."
    echo ""
    echo "서버 실행 방법:"
    echo "  ./gradlew bootRun"
    exit 1
fi

echo ""

# PowerShell 스크립트를 Bash에서 실행
echo "================================================"
echo "테스트 실행 중..."
echo "================================================"
echo ""
echo "Git Bash에서는 PowerShell 스크립트를 직접 실행할 수 없습니다."
echo "다음 중 하나의 방법을 선택하세요:"
echo ""
echo "방법 1: PowerShell에서 실행 (권장)"
echo "  1. 새 PowerShell 창 열기"
echo "  2. cd $(pwd)"
echo "  3. .\\qa_test_runner.ps1"
echo ""
echo "방법 2: Git Bash에서 PowerShell 호출"
echo "  powershell.exe -ExecutionPolicy Bypass -File ./qa_test_runner.ps1"
echo ""
echo "방법 3: Bash 스크립트로 간단 테스트 (아래 실행)"
echo ""

read -p "Bash 스크립트로 간단 테스트를 실행하시겠습니까? (y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "간단 테스트 실행 중..."
    bash ./qa_test_simple.sh
else
    echo ""
    echo "PowerShell에서 실행해주세요:"
    echo "  powershell.exe -ExecutionPolicy Bypass -File ./qa_test_runner.ps1"
fi
