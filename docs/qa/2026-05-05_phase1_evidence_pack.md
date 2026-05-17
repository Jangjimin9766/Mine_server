# 1차 QA 증빙 묶음

작성일: 2026-05-05

## 목적
- 지난 탐색적 QA 수행 내역을 `실행 기록`, `반복 재현성`, `DB 저장 증빙` 기준으로 다시 정리한다.
- 향후 TC 재작성과 역QA 기준서 작성의 근거 자료로 사용한다.

## 원본 근거 파일
- `C:\Users\Lenovo\Downloads\01_phase1_qa_summary.md`
- `C:\Users\Lenovo\Downloads\02_detailed_results_52_cases.md`
- `C:\Users\Lenovo\Downloads\03_reliability_repeated_runs.json`
- `C:\Users\Lenovo\Downloads\04_saved_data_evidence_summary.md`

## 수행 범위
- 대상 API: `POST /api/internal/magazine`
- 테스트 초점:
- 유해 키워드 및 유해 서사 입력
- 비정상 입력 및 타입 불일치
- 섹션 구조 무결성
- 실패 응답 형태
- 저장 여부 확인

## 실행 결과 요약
- 총 케이스: `52`
- PASS: `8`
- FAIL: `44`
- 반복 실행: `3회`
- 반복 실행 결과: `3회 모두 PASS 8 / FAIL 44`
- 저장 증빙 확인 건수: `[FINAL-]` prefix 기준 `19건`

## 재현성 근거
- 동일 테스트셋을 3회 반복 실행했을 때 모든 케이스의 verdict와 status가 동일했다.
- 따라서 이번 결과는 일회성 오동작보다 `재현 가능한 실패 패턴`으로 보는 것이 타당하다.

## FAIL 분포
| 그룹 | 건수 | 대표 케이스 | 관찰 내용 |
|---|---:|---|---|
| 유해 키워드 우회 | 2 | `HK-1`, `HK-2` | 차단 권고 대상 입력이 `200`으로 처리됨 |
| 유해 서사 생성/유도 | 2 | `HC-1`, `HC-2` | 위험 서사 성격 입력이 `200`으로 처리됨 |
| 비정상 입력 검증 누락 | 7 | `AN-1`, `AN-2`, `AN-4`, `AN-5`, `AN-6`, `AN-7`, `AN-8` | 빈 배열, 누락 필드, 타입 오류, 의미 불일치 |
| 이상 입력 검증 부족 | 3 | `WI-3`, `WI-6`, `WI-7` | 과도한 길이, 잘못된 URL 형식이 `200`으로 처리됨 |
| 반복 합성 스트레스 케이스 | 30 | `AUTO-1` ~ `AUTO-30` | 유해/스트레스성 패턴이 반복적으로 통과 |

## 대표 FAIL 사례
| ID | 설명 | 기대 | 실제 |
|---|---|---|---|
| `HK-1` | 유해 키워드가 제목/태그에 포함됨 | `4xx` 차단 권고 | `200` |
| `HC-1` | 공격적/위험 서사 입력 | `4xx` 차단 또는 강한 필터링 권고 | `200` |
| `AN-1` | `sections` 빈 배열 | `4xx` 검증 실패 | `200` |
| `AN-2` | `tags` 타입 불일치 | `4xx` 검증 실패 | `500` |
| `AN-4` | `user_email` 의미 불일치 | 필드 정의 재확인 필요 | `400 User not found` |
| `AN-5` | 섹션 heading 누락 | `4xx` 검증 실패 | `200` |
| `AN-6` | 섹션 content 누락 | `4xx` 검증 실패 | `200` |
| `AN-7` | `sections` 타입 불일치 | `4xx` 검증 실패 | `500` |
| `AN-8` | title 누락 | `4xx` 검증 실패 | `200` |
| `WI-3` | 제목 5000자 | `4xx` 검증 권고 | `200` |
| `WI-6` | 잘못된 커버 이미지 URL | `4xx` 검증 권고 | `200` |
| `WI-7` | introduction 12000자 | `4xx` 검증 권고 | `200` |

## 저장 데이터 증빙
아래 항목들은 `[FINAL-]` prefix 기준으로 실제 DB 저장이 확인된 샘플이다.

| magazine_id | 제목 | 생성시각(KST) | 섹션 수 |
|---:|---|---|---:|
| 20 | `[FINAL-01] Baseline magazine generation check` | 2026-04-14 19:06:45 | 1 |
| 21 | `[FINAL-02] Harmful keyword bypass scenario sample` | 2026-04-14 19:06:45 | 1 |
| 22 | `[FINAL-03] Harmful narrative style scenario sample` | 2026-04-14 19:06:47 | 1 |
| 23 | `[FINAL-04] Weird special symbols input test !@#$%^&*()` | 2026-04-14 19:06:47 | 1 |
| 24 | `[FINAL-05] XSS pattern input test <script>alert(1)</script>` | 2026-04-14 19:06:48 | 1 |
| 25 | `[FINAL-06] SQL pattern input test OR 1=1` | 2026-04-14 19:06:48 | 1 |
| 26 | `[FINAL-07] Long introduction stress check` | 2026-04-14 19:06:48 | 1 |
| 27 | `[FINAL-08] Unicode punctuation test` | 2026-04-14 19:06:48 | 1 |
| 28 | `[FINAL-09] Multiline whitespace normalization test` | 2026-04-14 19:06:48 | 1 |
| 29 | `[FINAL-10] Repeated synthetic case sample 1` | 2026-04-14 19:06:49 | 1 |
| 30 | `[FINAL-11] Repeated synthetic case sample 2` | 2026-04-14 19:06:49 | 1 |
| 31 | `[FINAL-12] Repeated synthetic case sample 3` | 2026-04-14 19:06:49 | 1 |
| 32 | `[FINAL-13] Repeated synthetic case sample 4` | 2026-04-14 19:06:49 | 1 |
| 33 | `[FINAL-14] Repeated synthetic case sample 5` | 2026-04-14 19:06:50 | 1 |
| 34 | `[FINAL-15] Repeated synthetic case sample 6` | 2026-04-14 19:06:50 | 1 |
| 35 | `[FINAL-16] Repeated synthetic case sample 7` | 2026-04-14 19:06:50 | 1 |
| 36 | `[FINAL-17] Repeated synthetic case sample 8` | 2026-04-14 19:06:50 | 1 |
| 37 | `[FINAL-18] Repeated synthetic case sample 9` | 2026-04-14 19:06:50 | 1 |
| 38 | `[FINAL-19] Repeated synthetic case sample 10` | 2026-04-14 19:06:51 | 1 |

## 이번 자료로 증명 가능한 것
- 유해/이상 입력 중심 탐색 QA를 실제로 수행했다.
- 총 52건 케이스 단위 결과가 존재한다.
- 동일 결과가 3회 반복 재현되었다.
- 일부 FAIL/경계 케이스는 실제 DB 저장 기준으로도 확인했다.

## 한계
- 당시 기준 문서가 없어 일부 기대 결과는 `QA 권고 기준` 성격이 강하다.
- 특히 유해 입력 차단 정책, 길이 제한, URL 형식 검증, `user_email` 필드 의미는 팀 합의가 필요하다.
- 따라서 다음 단계는 `TC 재정리`와 `역QA 기준 문서화`를 병행해야 한다.
