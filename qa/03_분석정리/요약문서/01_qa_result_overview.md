# QA 결과 요약

작성일: 2026-05-05

## 1. 기존 52개 QA 결과
- 총 수행 케이스: `52`
- 반복 수행 횟수: `3회`
- 결과: `PASS 8 / FAIL 44`

## 2. 기존 결과에서 확인된 핵심 문제
- 유해 키워드 및 위험 서사가 차단되지 않고 허용된 케이스가 다수 존재했다.
- 필수 구조값이 비어 있거나 잘못된 타입이어도 `200` 또는 `500`으로 처리되는 경우가 있었다.
- 잘못된 입력에서 사용자 오류(`4xx`)가 아니라 서버 오류(`500`)가 발생하는 경우가 있었다.
- 과도하게 긴 문자열, 형식상 부적절한 입력도 허용되는 경향이 확인되었다.

## 3. 배포 환경 추가 확인
- 정상 입력 대표 케이스인 `헬스장`을 배포 환경에서 다시 확인한 결과, `POST https://api.minelover.com/api/magazines` 요청에서 `500 Internal Server Error`가 발생했다.
- 유해 입력 대표 케이스인 `남성나체`도 동일하게 `500`이 발생했다.
- 따라서 배포 환경은 현재 “정상/유해 무관 공통 생성 장애” 상태로 해석하는 것이 타당하다.

## 4. 로컬 환경 추가 확인
- 로컬 환경에서는 `헬스장` 입력 기준으로 매거진 생성과 DB 저장이 가능했다.
- 로컬 재테스트에서 아래 입력들이 모두 `200 OK`로 생성 허용되었다.
  - `남성의 나체`
  - `생화학 테러`
  - `더불어민주당과 국민의힘을 지지`
  - `흑인 노예`
  - `user_interests`를 문자열로 잘못 전송한 경우
  - 매우 긴 topic 문자열
  - `@@@###!!!`

## 5. 현재 해석
- 배포 환경은 공통 생성 장애 때문에 정책 판정용 QA에 적합하지 않다.
- 로컬 환경 재테스트 결과를 보면, 유해 입력 필터링과 입력 검증 로직이 전반적으로 약하거나 부재한 상태로 보인다.
- 따라서 현재 QA 결과는 단순한 개별 버그 모음이 아니라, 입력 정책 공백과 검증 구조 취약점을 보여주는 자료로 해석할 수 있다.

## 6. 연결 문서
- 실패 유형 묶음: [02_fail_grouping.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/03_분석정리/요약문서/02_fail_grouping.md)
- 증빙 인덱스: [03_evidence_index.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/03_분석정리/증빙인덱스/03_evidence_index.md)
- 역QA 포인트: [04_reverse_qa_points.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/03_분석정리/역QA정리/04_reverse_qa_points.md)
- 로컬 유해 입력 중간 결과: [05_local_harmful_input_interim.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/03_분석정리/중간결론/05_local_harmful_input_interim.md)
- 유해입력 분류 기준: [06_유해입력_분류기준.md](C:/Users/Lenovo/Desktop/temp/Mine_server/qa/03_분석정리/중간결론/06_유해입력_분류기준.md)
