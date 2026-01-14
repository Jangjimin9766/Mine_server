#!/bin/bash
# M:ine API 간단 테스트 (Bash)
# Git Bash에서 실행 가능한 기본 테스트

BASE_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s)
USERNAME="testuser_$TIMESTAMP"
EMAIL="test_$TIMESTAMP@example.com"
PASSWORD="Test1234!@"
NICKNAME="Tester_$TIMESTAMP"  # 한글 대신 영문 사용 (인코딩 문제 방지)

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

# 테스트 함수
test_api() {
    local test_id=$1
    local description=$2
    local method=$3
    local endpoint=$4
    local data=$5
    local headers=$6
    local expected_status=${7:-200}
    
    echo ""
    echo -e "${CYAN}========================================"
    echo "[$test_id] $description"
    echo -e "========================================${NC}"
    echo "Method: $method $endpoint"
    
    # curl 명령 실행
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" $headers)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" $headers \
            -d "$data")
    fi
    
    # 상태 코드 추출
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    echo "Status: $status_code"
    echo "Response: $body"
    
    # 결과 확인
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((pass_count++))
        echo "$body"
    else
        echo -e "${RED}❌ FAIL - Expected $expected_status, got $status_code${NC}"
        ((fail_count++))
        echo ""
    fi
}

echo -e "${YELLOW}============================================"
echo "M:ine API 간단 테스트 (Bash)"
echo "============================================${NC}"
echo "Test User: $USERNAME"
echo "Test Email: $EMAIL"
echo ""

# ========================================
# 1. 회원가입
# ========================================

echo -e "\n${YELLOW}### 1. 인증 테스트 ###${NC}\n"

signup_data="{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"nickname\":\"$NICKNAME\"}"
signup_full_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/signup" \
    -H "Content-Type: application/json" \
    -d "$signup_data")

signup_status=$(echo "$signup_full_response" | tail -n1)
signup_response=$(echo "$signup_full_response" | sed '$d')

echo -e "${CYAN}[A-1] 회원가입${NC}"
echo "Request: $signup_data"
echo "Status: $signup_status"
echo "Response: $signup_response"

if [ "$signup_status" -eq 200 ] && echo "$signup_response" | grep -qE "^[0-9]+$"; then
    echo -e "${GREEN}✅ PASS${NC}"
    ((pass_count++))
    user_id=$signup_response
    echo "User ID: $user_id"
else
    echo -e "${RED}❌ FAIL - 회원가입 실패${NC}"
    echo -e "${YELLOW}⚠️  회원가입이 실패했으므로 이후 테스트를 건너뜁니다.${NC}"
    echo ""
    echo "가능한 원인:"
    echo "  1. 서버가 실행 중이 아님"
    echo "  2. 데이터베이스 연결 문제"
    echo "  3. 비밀번호 정책 불일치"
    echo "  4. 필수 필드 누락"
    echo ""
    echo "서버 로그를 확인해주세요!"
    ((fail_count++))
    exit 1
fi

sleep 1

# ========================================
# 2. 로그인
# ========================================

login_data="{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}"
login_response=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$login_data")

echo ""
echo -e "${CYAN}[A-2] 로그인${NC}"
echo "Response: $login_response"

# accessToken 추출 (jq가 없으면 grep/sed 사용)
if command -v jq &> /dev/null; then
    access_token=$(echo "$login_response" | jq -r '.accessToken')
    refresh_token=$(echo "$login_response" | jq -r '.refreshToken')
else
    access_token=$(echo "$login_response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    refresh_token=$(echo "$login_response" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
fi

if [ -n "$access_token" ] && [ "$access_token" != "null" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "Access Token: ${access_token:0:20}..."
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL - No access token${NC}"
    ((fail_count++))
    exit 1
fi

sleep 1

# ========================================
# 3. 내 프로필 조회
# ========================================

echo ""
echo -e "${CYAN}[U-1] 내 프로필 조회${NC}"
profile_response=$(curl -s -X GET "$BASE_URL/users/me" \
    -H "Authorization: Bearer $access_token")

echo "Response: $profile_response"

if echo "$profile_response" | grep -q "$USERNAME"; then
    echo -e "${GREEN}✅ PASS${NC}"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL${NC}"
    ((fail_count++))
fi

sleep 1

# ========================================
# 4. 매거진 생성 (내부 API)
# ========================================

echo ""
echo -e "\n${YELLOW}### 2. 매거진 테스트 ###${NC}\n"

magazine_data="{\"title\":\"Test Magazine $TIMESTAMP\",\"subtitle\":\"Test Subtitle\",\"introduction\":\"Simple Test\",\"cover_image_url\":\"https://example.com/cover.jpg\",\"user_email\":\"$EMAIL\",\"sections\":[{\"heading\":\"Section1\",\"content\":\"Content\",\"image_url\":\"https://example.com/img.jpg\",\"layout_type\":\"basic\"}]}"
echo -e "${CYAN}[M-1] 매거진 생성${NC}"
magazine_response=$(curl -s -X POST "$BASE_URL/internal/magazine" \
    -H "Content-Type: application/json" \
    -H "X-Internal-Key: mine-secret-key-1234" \
    -d "$magazine_data")

echo "Response: $magazine_response"

if command -v jq &> /dev/null; then
    magazine_id=$(echo "$magazine_response" | jq -r '.magazineId')
else
    magazine_id=$(echo "$magazine_response" | grep -o '"magazineId":[0-9]*' | cut -d':' -f2)
fi

if [ -n "$magazine_id" ] && [ "$magazine_id" != "null" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "Magazine ID: $magazine_id"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL${NC}"
    ((fail_count++))
fi

sleep 1

# ========================================
# 5. 내 매거진 목록 조회
# ========================================

echo ""
echo -e "${CYAN}[M-2] 내 매거진 목록 조회${NC}"
my_magazines=$(curl -s -X GET "$BASE_URL/magazines/my?page=0&size=10" \
    -H "Authorization: Bearer $access_token")

echo "Response: ${my_magazines:0:200}..."

if echo "$my_magazines" | grep -q "content"; then
    echo -e "${GREEN}✅ PASS${NC}"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL${NC}"
    ((fail_count++))
fi

sleep 1

# ========================================
# 6. 에러 테스트: 중복 회원가입
# ========================================

echo ""
echo -e "\n${YELLOW}### 3. 에러 시나리오 테스트 ###${NC}\n"

echo -e "${CYAN}[E-A1] 중복 아이디 회원가입 (에러 예상)${NC}"
duplicate_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/signup" \
    -H "Content-Type: application/json" \
    -d "$signup_data")

duplicate_status=$(echo "$duplicate_response" | tail -n1)
echo "Status: $duplicate_status"

if [ "$duplicate_status" -eq 400 ]; then
    echo -e "${GREEN}✅ PASS - Expected error occurred${NC}"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL - Expected 400, got $duplicate_status${NC}"
    ((fail_count++))
fi

sleep 1

# ========================================
# 7. 에러 테스트: 인증 없이 API 호출
# ========================================

echo ""
echo -e "${CYAN}[E-A7] 인증 없이 보호된 API 호출 (에러 예상)${NC}"
unauth_response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/magazines/my?page=0&size=10")

unauth_status=$(echo "$unauth_response" | tail -n1)
echo "Status: $unauth_status"

if [ "$unauth_status" -eq 401 ] || [ "$unauth_status" -eq 403 ]; then
    echo -e "${GREEN}✅ PASS - Expected error occurred${NC}"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL - Expected 401/403, got $unauth_status${NC}"
    ((fail_count++))
fi

sleep 1

# ========================================
# 8. 로그아웃
# ========================================

echo ""
echo -e "${CYAN}[A-4] 로그아웃${NC}"
logout_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/logout" \
    -H "Authorization: Bearer $access_token")

logout_status=$(echo "$logout_response" | tail -n1)
echo "Status: $logout_status"

if [ "$logout_status" -eq 200 ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    ((pass_count++))
else
    echo -e "${RED}❌ FAIL${NC}"
    ((fail_count++))
fi

# ========================================
# 결과 요약
# ========================================

echo ""
echo ""
echo -e "${YELLOW}============================================"
echo "테스트 결과 요약"
echo "============================================${NC}"
echo "총 테스트: $((pass_count + fail_count))"
echo -e "${GREEN}성공: $pass_count${NC}"
echo -e "${RED}실패: $fail_count${NC}"
echo ""

if [ $fail_count -gt 0 ]; then
    echo -e "${YELLOW}⚠️  실패한 테스트가 있습니다. 상세 내용을 확인하세요.${NC}"
    echo ""
fi

echo -e "${CYAN}전체 테스트를 실행하려면 PowerShell을 사용하세요:"
echo "  powershell.exe -ExecutionPolicy Bypass -File ./qa_test_runner.ps1${NC}"
echo ""
