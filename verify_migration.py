import requests
import json
import os
import sys

BASE_URL = "https://api.minelover.com"
USERNAME = "mine_lover"
PASSWORD = "Mypassword1!"

def test_magazine_lifecycle():
    print(f"--- [1] Login (username: {USERNAME}) ---")
    login_res = requests.post(f"{BASE_URL}/api/auth/login", json={"username": USERNAME, "password": PASSWORD})
    if login_res.status_code != 200:
        print(f"❌ Login failed: {login_res.text}")
        return
    token = login_res.json().get("accessToken")
    print("✅ Login Success.")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    print("\n--- [2] Creating New Magazine (Topic: Specialty Coffee) ---")
    create_res = requests.post(f"{BASE_URL}/api/magazines", headers=headers, json={"topic": "스페셜티 커피의 세계", "user_mood": "minimal and refined"})
    if create_res.status_code != 200:
        print(f"❌ Create failed: {create_res.text}")
        return
    
    try:
        mag_id = create_res.json()
        if isinstance(mag_id, dict):
            mag_id = mag_id.get("magazineId")
    except:
        mag_id = create_res.text.strip()
        
    print(f"✅ SUCCESS: Created Magazine ID: {mag_id}")

    print("\n--- [3] AI Interaction: Add Section (Topic: Dessert Pairing) ---")
    interact_res = requests.post(f"{BASE_URL}/api/magazines/{mag_id}/interact", headers=headers, json={"message": "디저트 페어링 섹션 하나 추가해줘"})
    if interact_res.status_code != 200:
        print(f"❌ Interaction failed: {interact_res.text}")
        return
    print(f"✅ AI Interaction Success (Intent: {interact_res.json().get('actionType')})")

    print("\n--- [4] Verifying Core Structures (3 Paragraphs & S3 Integration) ---")
    detail_res = requests.get(f"{BASE_URL}/api/magazines/{mag_id}", headers=headers)
    if detail_res.status_code != 200:
        print(f"❌ Failed to fetch detail: {detail_res.text}")
        return
    
    data = detail_res.json()
    sections = data.get("sections", [])
    print(f"Magazine Title: {data.get('title')}")
    print(f"Total Sections Found: {len(sections)}")

    all_passed = True
    for i, section in enumerate(sections):
        paragraphs = section.get("paragraphs", [])
        is_s3 = all("s3" in (p.get("imageUrl") or "").lower() for p in paragraphs)
        
        status_str = f"Section {i} ('{section.get('heading')}'): {len(paragraphs)} paragraphs"
        if len(paragraphs) == 3 and is_s3:
            print(f"  ✅ {status_str} (Structure OK + S3 OK)")
        else:
            print(f"  ❌ {status_str} (FAIL: paragraphs={len(paragraphs)}, s3={is_s3})")
            all_passed = False

    if all_passed:
        print("\n🏆 MIGRATION VERIFIED: Everything is perfect!")
    else:
        print("\n⚠️ MIGRATION ALERT: Some structural integrity issues found.")

    print("\n--- [Cleanup] Deleting Test Magazine ---")
    del_res = requests.delete(f"{BASE_URL}/api/magazines/{mag_id}", headers=headers)
    print(f"✅ Cleanup complete (Status: {del_res.status_code})")

if __name__ == "__main__":
    test_magazine_lifecycle()
