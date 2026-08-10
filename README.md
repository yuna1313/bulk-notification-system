# bulk-notification-system

예약 대량 알림 발송 시스템입니다.

동기 처리 방식(v1)의 한계를 부하 테스트로 직접 측정한 뒤, Kafka 기반 비동기 구조(v2)로 전환하여 **두 구조를 동일한 조건에서 수치로 비교**하는 것을 목표로 합니다.

> 진행 상황: **v1 (동기 버전) 개발 중**

## 왜 두 번 만드는가

"대용량 처리에는 메시지 큐가 필요하다"는 일반론 대신, **어느 지점에서 왜 무너지는지를 직접 측정한 데이터**를 근거로 삼기 위해서입니다.

v1은 성능이 나쁜 것이 의도된 설계입니다. 발송 요청을 API 스레드 안에서 수신자 수만큼 순차 호출하며, 재시도나 비동기 처리를 넣지 않습니다. 이 구조가 부하에서 어떻게 무너지는지를 기록한 뒤, 그 지표를 개선 목표로 두고 v2를 만듭니다.

v1은 v2 완성 후에도 삭제하지 않고 `v1.0-sync` 태그로 보존합니다.

## 시스템 구성

| 모듈 | 역할 |
|---|---|
| `notification-api` | 발송 요청 접수, 발송 현황 조회. v1에서는 발송 실행까지 담당 |
| `notification-worker` | Kafka 컨슈머. 실제 발송 처리 (v2에서 추가) |
| `mock-provider` | 외부 발송사를 흉내 내는 테스트용 서버 · [문서](./mock-provider/README.md) |
| `load-test` | k6 부하 테스트 스크립트 |
| `docs` | 부하 테스트 결과 및 분석 |

`mock-provider`는 실제 발송사 API를 대신합니다. 지연시간과 실패율을 실행 중에 조작할 수 있어야 장애 상황을 재현할 수 있기 때문에 별도 서버로 분리했습니다.

## 기술 스택

Java 17 · Spring Boot 4.1.0 · Spring Data JPA · MySQL 8 · Gradle · k6

v2에서 Apache Kafka, Prometheus, Grafana가 추가됩니다.

## 실행

로컬에 MySQL 8이 실행 중이어야 합니다. 최초 1회 스키마와 계정을 준비합니다.  
(아래 아이디와 비밀번호는 예시입니다.)

```sql
CREATE DATABASE notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'notification'@'localhost' IDENTIFIED BY 'notification';
GRANT ALL PRIVILEGES ON notification.* TO 'notification'@'localhost';
```

기존 계정을 쓰려면 `DB_USERNAME`, `DB_PASSWORD` 환경변수로 덮어씁니다.

```bash
# 가짜 발송사 서버 (포트 8081)
cd mock-provider && ./gradlew bootRun

# 알림 발송 서버 (포트 8080)
cd notification-api && ./gradlew bootRun
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/notifications` | 발송 요청 등록 (수신자 목록 + 예약 시각) |
| `POST` | `/api/notifications/{id}/dispatch` | 발송 실행 |
| `GET` | `/api/notifications/{id}` | 발송 현황 조회 (성공 · 실패 · 대기 건수) |

## 진행 단계

**v1 — 동기 처리**

- [x] mock-provider 구현 (지연 · 실패율 · rate limit 조절)
- [x] 도메인 설계 및 발송 요청 API
- [x] 동기 발송 로직
- [x] 부하 테스트 및 결과 기록

**v2 — Kafka 기반 비동기 처리**

- [ ] Kafka 구성 및 토픽 설계
- [ ] Transactional Outbox 패턴으로 이벤트 발행
- [ ] 발송 컨슈머 분리
- [ ] 멱등성 처리 (중복 발송 차단)
- [ ] 재시도 및 DLQ
- [ ] 컨슈머 lag 모니터링
- [ ] 구간 재처리(replay) 어드민 API

## 측정 결과

v1 측정을 마쳤습니다. 전체 기록은 [`docs/load-test-v1.md`](./docs/load-test-v1.md)에 있습니다.

| 지표 | v1 |
|---|---|
| 100,000건 발송 소요시간 | **6시간 19분** |
| 발송 처리량 | 4.4건/초 (규모와 무관하게 고정) |
| 발송사 지연 전파 | 1:1 (지연 3초 시 10만 건 85시간) |
| 발송 중 접수 API p95 | 동시 발송 200건에서 **56.99초** |
| 실패 건 처리 | 재시도 없음, 2.97% 전량 유실 |

v1의 한계는 특정 규모에서 무너지는 것이 아니라 **구조적으로 일정 속도 이상 낼 수 없다**는 데 있습니다.
수신자를 1,000배 늘려도 건당 비용은 1.2%만 증가했고, 10만 행이 쌓인 상태에서도 인덱스는 정상 동작했습니다.
구현 결함이 아니라 순차 호출이라는 구조가 병목입니다.

건당 226ms 중 200ms가 발송사 응답 대기이며 이 시간 동안 CPU는 유휴 상태입니다.
순수한 I/O 대기이므로 병렬화로 단축할 수 있고, v2는 이 지점을 목표로 합니다.

(v2) 파티션 수에 따른 처리량 변화, 부하 종료 후 컨슈머 lag 회복 시간을 추가로 측정합니다.