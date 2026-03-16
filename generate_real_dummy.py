import urllib.request
import urllib.error
import json
import random
import time

# API endpoint configured for the Production Spring Boot server (HTTPS)
BASE_URL = "https://api.minelover.com/api/auth/signup"

# Users to create (simulating the ones previously hardcoded in data.sql)
USERS = [
    {
        "username": "jiwoo_kim",
        "email": "jiwoo@example.com",
        "password": "Password123!",
        "nickname": "지우의일상"
    },
    {
        "username": "minjun_lee",
        "email": "minjun@example.com",
        "password": "Password123!",
        "nickname": "민준테크"
    },
    {
        "username": "seoyun_park",
        "email": "seoyun@example.com",
        "password": "Password123!",
        "nickname": "서윤아트"
    },
    {
        "username": "hyejin_choi",
        "email": "hyejin@example.com",
        "password": "Password123!",
        "nickname": "혜진의서재"
    },
    {
        "username": "taeyang_lee",
        "email": "taeyang@example.com",
        "password": "Password123!",
        "nickname": "태양스포츠"
    }
]

# Available interest codes based on data.sql
INTEREST_CODES = [
    "FASHION", "BEAUTY", "INTERIOR", "MUSIC", "ART", "OTT", "MOVIE",
    "MINIMALISM", "TREND", "SPORTS", "TRAVEL", "CAMPING", "IT", "ELECTRONICS", "FOOD", "TECH"
]

def create_user(user_data):
    # Select 2 random interests
    selected_interests = random.sample(INTEREST_CODES, 2)
    
    payload = {
        "username": user_data["username"],
        "email": user_data["email"],
        "password": user_data["password"],
        "nickname": user_data["nickname"],
        "interests": selected_interests
    }
    
    data_bytes = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(BASE_URL, data=data_bytes, headers={'Content-Type': 'application/json'})
    
    print(f"🚀 가입 요청 전송 중: {user_data['nickname']} ({user_data['username']}) - 관심사: {selected_interests}")
    
    try:
        response = urllib.request.urlopen(req)
        if response.status in (200, 201):
            print(f"✅ 회원가입 성공! (AI 서버에서 비동기로 매거진을 생성하기 시작합니다.)")
        else:
            print(f"⚠️ 예상치 못한 응답 상태 코드: {response.status}")
    except urllib.error.HTTPError as e:
        error_info = e.read().decode('utf-8')
        # If the user already exists, it might throw a 400 or 409
        print(f"❌ 실패 (HTTP {e.code}): {error_info}")
    except urllib.error.URLError as e:
        print(f"❌ 연결 실패 (서버가 켜져 있는지 확인하세요): {e.reason}")

def main():
    print("=========================================================")
    print("🌟 MINE AI Magazine Dummy Data Generator 🌟")
    print("이 스크립트는 5명의 가상 유저를 회원가입시킵니다.")
    print("가입과 동시에 Spring Event가 발생하여 AI 서버(magazine_maker)가")
    print("각 유저의 관심사에 맞는 실제 매거진을 자동 생성합니다.")
    print("=========================================================\n")
    
    for user_data in USERS:
        create_user(user_data)
        # Give a small pause between requests
        time.sleep(2)
        
    print("\n🎉 모든 가입 요청 완료!")
    print("참고: AI 서버가 매거진을 완전히 생성하기까지는 계정당 약 1~2분 정도 소요됩니다.")
    print("서버 로그를 확인해 매거진 생성 진행 상황을 지켜보세요.")

if __name__ == "__main__":
    main()
