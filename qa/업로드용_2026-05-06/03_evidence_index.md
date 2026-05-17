# 증빙 인덱스

작성일: 2026-05-05

## 1. 배포 환경 정상 입력 실패 증빙
주제: 헬스장
- [FINAL-001_fail_popup.png](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/정상입력_대표케이스/헬스장/화면캡처/FINAL-001_fail_popup.png)
- [FINAL-001_headers.png](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/정상입력_대표케이스/헬스장/네트워크/FINAL-001_headers.png)
- [FINAL-001_payload.png](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/정상입력_대표케이스/헬스장/네트워크/FINAL-001_payload.png)
- [FINAL-001_response.png](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/정상입력_대표케이스/헬스장/네트워크/FINAL-001_response.png)
- [FINAL-001_헬스장.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/정상입력_대표케이스/헬스장/메모/FINAL-001_헬스장.txt)

## 2. 배포 환경 유해 입력 장애 증빙
- [HK-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/유해입력/메모/HK-1.txt)
- [REPRESENTATIVE_CASES.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/01_배포환경/유해입력/메모/REPRESENTATIVE_CASES.md)

## 3. 로컬 환경 정상 입력 성공 증빙
주제: 헬스장
- [LOCAL-001_헬스장_로컬생성성공.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/정상입력_대표케이스/헬스장/메모/LOCAL-001_헬스장_로컬생성성공.txt)

## 4. 로컬 환경 유해/비정상 입력 재테스트 증빙

### 실제표현
- [HK-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/실제표현/성적/HK-1.txt)
- [HC-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/실제표현/위험폭력/HC-1.txt)
- [POL-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/실제표현/정치선동/POL-1.txt)
- [DIS-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/실제표현/차별혐오/DIS-1.txt)

### 구조오류/형식오류
- [AN-2.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/구조오류/AN-2.txt)
- [WI-3.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/긴문자열_형식오류/WI-3.txt)
- [SYM-1.txt](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/02_로컬환경/유해입력/긴문자열_형식오류/SYM-1.txt)

## 5. 현재 해석
- 배포 환경에서는 정상 입력과 유해 입력 모두 공통 500 오류가 발생해 정책 판정이 어렵다.
- 로컬 환경에서는 생성 성공이 가능했기 때문에, 입력 정책과 검증 로직 부재를 확인하는 용도로 활용 가능하다.
