import requests
import time
import json

API_KEY = "HRUIGXND53S7824B7OTNFR43J1E46H1E94D8V56Z"
ENDPOINT = "https://api.runpod.ai/v2/n47092i77bmns0/run"

headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

payload = {
    "input": {
        "action": "generate_magazine",
        "topic": "세계여행",
        "user_mood": ""
    }
}

print("Starting RunPod API Request...")
try:
    response = requests.post(ENDPOINT, headers=headers, json=payload)
    if response.status_code != 200:
        print(f"Failed to submit: {response.status_code}")
        print(response.text)
        exit(1)
        
    data = response.json()
    job_id = data.get("id")
    print(f"Job ID: {job_id}")

    status_url = f"https://api.runpod.ai/v2/n47092i77bmns0/status/{job_id}"
    while True:
        status_response = requests.get(status_url, headers=headers)
        status_data = status_response.json()
        status = status_data.get("status")
        print(f"Status: {status}")
        
        if status in ["COMPLETED", "FAILED"]:
            print("="*40)
            print(json.dumps(status_data, indent=2, ensure_ascii=False))
            break
        time.sleep(2)
except Exception as e:
    print(f"Python Error: {e}")
